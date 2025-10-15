#!/usr/bin/env bash
# Schedule '/home/joyfulday/.nvm/versions/node/v22.19.0/bin/claude' (or any command) at a specified time.
# Default backend: user-level systemd transient timers via systemd-run.
# Extras:
#   --open-terminal   Launch a terminal window and run claude interactively (requires GUI)
#   --terminal NAME   Choose terminal: gnome-terminal|konsole|xfce4-terminal|xterm|kitty|alacritty
#   --tmux            Run claude in a detached tmux session (no GUI). Attach with: tmux attach -t <name>
#   --tmux-session S  Name for the tmux session (default auto-generated)
#
# Examples:
#   One-time today 12:35, open a new terminal window:
#      ./schedule-claude.sh --mode once --time "14:01" --open-terminal --terminal xterm
#     ./schedule-claude.sh --mode once --time "12:35" --open-terminal
#
#   One-time today 12:35, in tmux:
#     ./schedule-claude.sh --mode once --time "12:35" --tmux
#
#   Daily 07:45, open terminal:
#     ./schedule-claude.sh --mode daily --time "07:45" --open-terminal
#
#   Weekly Mon 09:00 via cron backend (recurring, non-interactive by default):
#     ./schedule-claude.sh --mode weekly --day Mon --time "09:00" --use-cron
set -euo pipefail

print_help() {
  cat <<'EOF'
Schedule the default claude binary (/home/joyfulday/.nvm/versions/node/v22.19.0/bin/claude) or any command using systemd timers or cron.

Required:
  --mode MODE               one of: once, daily, weekly, cron

Time specification:
  --time "HH:MM"            time of day for once/daily/weekly (e.g., "18:30")
  --time "YYYY-MM-DD HH:MM" exact date-time for --mode once (e.g., "2025-10-11 09:15")
  --day DOW                 day of week for --mode weekly (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
  --cron "CRON_EXPR"        cron schedule (5 fields) for --mode cron (e.g., "*/15 * * * *")

Other options:
  --unit NAME               optional unit name (for systemd timers)
  --persistent              make the systemd timer persistent (catch up missed runs)
  --use-cron                use cron instead of systemd-run (not supported for --mode once)
  --node PATH               absolute path to node; prefixes "node PATH" before the command
  --open-terminal           open a terminal window and run the command there (interactive)
  --terminal NAME           choose terminal: gnome-terminal|konsole|xfce4-terminal|xterm|kitty|alacritty
  --tmux                    run in a detached tmux session (interactive; attach later)
  --tmux-session NAME       name for the tmux session (default auto-generated)
  --dry-run                 print what would be executed without applying
  --help                    show this help

Command:
  After --, you may pass the command to run.
  Default command: /home/joyfulday/.nvm/versions/node/v22.19.0/bin/claude
EOF
}

# Defaults
MODE=""
TIME_STR=""
DOW=""
CRON_EXPR=""
UNIT_NAME=""
USE_CRON="false"
PERSISTENT="false"
DRY_RUN="false"
NODE_PATH=""
OPEN_TERMINAL="false"
TERMINAL_NAME=""
USE_TMUX="false"
TMUX_SESSION=""
COMMAND=()

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode) MODE="${2:-}"; shift 2 ;;
    --time) TIME_STR="${2:-}"; shift 2 ;;
    --day) DOW="${2:-}"; shift 2 ;;
    --cron) CRON_EXPR="${2:-}"; shift 2 ;;
    --unit) UNIT_NAME="${2:-}"; shift 2 ;;
    --use-cron) USE_CRON="true"; shift ;;
    --persistent) PERSISTENT="true"; shift ;;
    --node) NODE_PATH="${2:-}"; shift 2 ;;
    --open-terminal) OPEN_TERMINAL="true"; shift ;;
    --terminal) TERMINAL_NAME="${2:-}"; shift 2 ;;
    --tmux) USE_TMUX="true"; shift ;;
    --tmux-session) TMUX_SESSION="${2:-}"; shift 2 ;;
    --dry-run) DRY_RUN="true"; shift ;;
    --help|-h) print_help; exit 0 ;;
    --) shift; COMMAND=("$@"); break ;;
    *) echo "Unknown argument: $1"; echo; print_help; exit 1 ;;
  esac
done

# Default command to your absolute claude path if none provided
if [[ ${#COMMAND[@]} -eq 0 ]]; then
  COMMAND=("/home/joyfulday/.nvm/versions/node/v22.19.0/bin/claude")
fi

# Validate options
if [[ "$OPEN_TERMINAL" == "true" && "$USE_TMUX" == "true" ]]; then
  echo "Choose either --open-terminal or --tmux, not both."
  exit 1
fi

require_arg() {
  local name="$1"; local val="$2"
  if [[ -z "$val" ]]; then
    echo "Missing required argument: $name"; echo; print_help; exit 1
  fi
}

# Validate mode
case "$MODE" in
  once)   require_arg "--time" "$TIME_STR" ;;
  daily)  require_arg "--time" "$TIME_STR" ;;
  weekly) require_arg "--time" "$TIME_STR"; require_arg "--day" "$DOW" ;;
  cron)   require_arg "--cron" "$CRON_EXPR" ;;
  *) echo "Invalid or missing --mode. Must be one of: once, daily, weekly, cron"; echo; print_help; exit 1 ;;
esac

# Helper: normalize day-of-week to Mon/Tue/... form
normalize_dow() {
  local d="$(echo "$1" | tr '[:upper:]' '[:lower:]')"
  case "$d" in
    mon|monday|1) echo "Mon" ;;
    tue|tuesday|2) echo "Tue" ;;
    wed|wednesday|3) echo "Wed" ;;
    thu|thursday|4|thur|thurs) echo "Thu" ;;
    fri|friday|5) echo "Fri" ;;
    sat|saturday|6) echo "Sat" ;;
    sun|sunday|0|7) echo "Sun" ;;
    *) echo "INVALID" ;;
  esac
}

# Build schedule strings
ON_CALENDAR=""
CRON_LINE=""

# Determine if systemd-run is available
have_systemd_run="false"
if command -v systemd-run >/dev/null 2>&1; then
  have_systemd_run="true"
fi

# Build schedule
if [[ "$MODE" == "once" ]]; then
  if [[ "$USE_CRON" == "true" ]]; then
    echo "--mode once is not supported with --use-cron. Use systemd timers for one-time schedules."
    exit 1
  fi
  if [[ "$TIME_STR" =~ ^[0-9]{1,2}:[0-9]{2}$ ]]; then
    now_ts=$(date +%s)
    today_date=$(date +%F)
    target_ts=$(date -d "$today_date $TIME_STR" +%s)
    if (( target_ts <= now_ts )); then
      ON_CALENDAR="$(date -d "tomorrow $TIME_STR" +"%Y-%m-%d %H:%M:00")"
    else
      ON_CALENDAR="$today_date ${TIME_STR}:00"
    fi
  else
    # full datetime
    if [[ "$TIME_STR" =~ :[0-9]{2}$ ]]; then
      ON_CALENDAR="$TIME_STR:00"
    else
      ON_CALENDAR="$TIME_STR"
    fi
  fi
elif [[ "$MODE" == "daily" ]]; then
  [[ "$TIME_STR" =~ ^[0-9]{1,2}:[0-9]{2}$ ]] || { echo "--time must be HH:MM for --mode daily"; exit 1; }
  ON_CALENDAR="*-*-* ${TIME_STR}:00"
elif [[ "$MODE" == "weekly" ]]; then
  [[ "$TIME_STR" =~ ^[0-9]{1,2}:[0-9]{2}$ ]] || { echo "--time must be HH:MM for --mode weekly"; exit 1; }
  ndow=$(normalize_dow "$DOW"); [[ "$ndow" != "INVALID" ]] || { echo "Invalid --day"; exit 1; }
  ON_CALENDAR="$ndow ${TIME_STR}:00"
elif [[ "$MODE" == "cron" ]]; then
  :
fi

# Build command string (with optional node prefix)
join_args() { local IFS=' '; printf '%s' "$*"; }
CMD_ARRAY=("${COMMAND[@]}")
CMD_STR="$(join_args "${CMD_ARRAY[@]}")"

# If user provided a node path, prefix it
if [[ -n "$NODE_PATH" ]]; then
  if [[ ! -x "$NODE_PATH" ]]; then
    echo "Error: --node path is not executable: $NODE_PATH"; exit 1
  fi
  CMD_STR="$NODE_PATH $CMD_STR"
else
  # Auto-detect nvm install and prefix sibling node
  cmd0="${CMD_ARRAY[0]}"
  if [[ "$cmd0" == *"/.nvm/"*"/bin/claude" ]]; then
    candidate_node="$(dirname "$cmd0")/node"
    if [[ -x "$candidate_node" ]]; then
      CMD_STR="$candidate_node $CMD_STR"
    fi
  fi
fi

# If interactive terminal requested, wrap CMD_STR in a terminal emulator command
escape_squote() { printf "%s" "$1" | sed "s/'/'\"'\"'/g"; }

if [[ "$OPEN_TERMINAL" == "true" ]]; then
  # Determine terminal emulator
  chosen="$TERMINAL_NAME"
  if [[ -z "$chosen" ]]; then
    for t in gnome-terminal konsole xfce4-terminal xterm kitty alacritty; do
      if command -v "$t" >/dev/null 2>&1; then chosen="$t"; break; fi
    done
  fi
  if [[ -z "$chosen" ]]; then
    echo "No supported terminal emulator found. Install one (e.g., gnome-terminal, xterm) or use --tmux."
    exit 1
  fi

  cmd_esc="$(escape_squote "$CMD_STR")"
  case "$chosen" in
    gnome-terminal)
      # Keeps window open after command finishes
      CMD_STR="gnome-terminal -- bash -lc '$cmd_esc; exec bash'"
      ;;
    konsole)
      CMD_STR="konsole --noclose -e bash -lc '$cmd_esc'"
      ;;
    xfce4-terminal)
      CMD_STR="xfce4-terminal --hold -e bash -lc '$cmd_esc'"
      ;;
    xterm)
      CMD_STR="xterm -hold -e bash -lc '$cmd_esc'"
      ;;
    kitty)
      # kitty supports --hold
      CMD_STR="kitty --hold bash -lc '$cmd_esc'"
      ;;
    alacritty)
      CMD_STR="alacritty -e bash -lc '$cmd_esc; exec bash'"
      ;;
    *)
      echo "Unsupported terminal: $chosen"; exit 1
      ;;
  esac
fi

# If tmux requested, wrap CMD_STR in a tmux new-session
if [[ "$USE_TMUX" == "true" ]]; then
  if ! command -v tmux >/dev/null 2>&1; then
    echo "tmux is not installed. Install it (sudo apt install tmux) or use --open-terminal."
    exit 1
  fi
  if [[ -z "$TMUX_SESSION" ]]; then
    TMUX_SESSION="claude-$(id -un)-$(date +%Y%m%d%H%M%S)"
  fi
  cmd_esc="$(escape_squote "$CMD_STR")"
  # Start a detached tmux session that runs our command in a login shell and keeps the shell if it exits
  CMD_STR="tmux new-session -d -s '$TMUX_SESSION' bash -lc '$cmd_esc; exec bash'"
fi

# Build cron line if needed
if [[ "$MODE" == "cron" ]]; then
  require_arg "--cron" "$CRON_EXPR"
  CRON_LINE="$CRON_EXPR /usr/bin/env bash -lc '${CMD_STR}' # schedule-claude"
fi

# Choose backend
if [[ "$USE_CRON" == "true" ]]; then
  if [[ "$MODE" != "cron" ]]; then
    if [[ "$MODE" == "daily" ]]; then
      mm="${TIME_STR#*:}"; hh="${TIME_STR%:*}"
      CRON_LINE="$mm $hh * * * /usr/bin/env bash -lc '${CMD_STR}' # schedule-claude"
    elif [[ "$MODE" == "weekly" ]]; then
      mm="${TIME_STR#*:}"; hh="${TIME_STR%:*}"
      case "$(normalize_dow "$DOW")" in
        Sun) dow_c=0 ;; Mon) dow_c=1 ;; Tue) dow_c=2 ;; Wed) dow_c=3 ;;
        Thu) dow_c=4 ;; Fri) dow_c=5 ;; Sat) dow_c=6 ;; *) echo "Invalid --day for cron"; exit 1 ;;
      esac
      CRON_LINE="$mm $hh * * $dow_c /usr/bin/env bash -lc '${CMD_STR}' # schedule-claude"
    fi
  fi
  if [[ "$DRY_RUN" == "true" ]]; then
    echo "[DRY RUN] Would install the following cron entry:"; echo "$CRON_LINE"; exit 0
  fi
  tmpfile="$(mktemp)"
  crontab -l 2>/dev/null | grep -v '# schedule-claude' > "$tmpfile" || true
  echo "$CRON_LINE" >> "$tmpfile"
  crontab "$tmpfile"; rm -f "$tmpfile"
  echo "Cron schedule installed. View with: crontab -l"
  if [[ "$USE_TMUX" == "true" ]]; then
    echo "tmux session will be named: $TMUX_SESSION"
  fi
  exit 0
fi

# systemd-run path
if [[ "$have_systemd_run" != "true" ]]; then
  echo "systemd-run not found. Install systemd or use --use-cron with --mode cron."
  exit 1
fi

# Unit name
if [[ -z "$UNIT_NAME" ]]; then
  ts="$(date +%Y%m%d%H%M%S)"
  UNIT_NAME="claude-$(id -un)-$MODE-$ts"
fi

# Persistent timer property
timer_props=("AccuracySec=1s")
if [[ "$PERSISTENT" == "true" ]]; then
  timer_props+=("Persistent=true")
fi

# Build systemd-run command
if [[ -n "$ON_CALENDAR" ]]; then
  RUN_CMD=(systemd-run --user --unit "$UNIT_NAME" --on-calendar "$ON_CALENDAR" --collect)
  for p in "${timer_props[@]}"; do RUN_CMD+=(--timer-property "$p"); done
  RUN_CMD+=(bash -lc "${CMD_STR}")
else
  echo "Internal error: ON_CALENDAR is empty."; exit 1
fi

if [[ "$DRY_RUN" == "true" ]]; then
  echo "[DRY RUN] Would run:"; printf '  '; printf '%q ' "${RUN_CMD[@]}"; echo
  echo "[DRY RUN] Expanded command:"; echo "  ${CMD_STR}"
  if [[ "$USE_TMUX" == "true" ]]; then echo "tmux session: $TMUX_SESSION"; fi
  exit 0
fi

# Execute
"${RUN_CMD[@]}"

echo "Created user-level systemd timer/service:"
echo "  Unit:      $UNIT_NAME.service"
echo "  Timer:     $UNIT_NAME.timer"
echo "  Schedule:  $ON_CALENDAR"
if [[ "$PERSISTENT" == "true" ]]; then echo "  Persistent: yes"; fi
if [[ "$OPEN_TERMINAL" == "true" ]]; then echo "  Action:     open terminal and run claude"; fi
if [[ "$USE_TMUX" == "true" ]]; then echo "  Action:     start tmux session '$TMUX_SESSION'"; fi
echo
echo "Manage and inspect:"
echo "  systemctl --user list-timers | grep $UNIT_NAME"
echo "  systemctl --user status $UNIT_NAME.timer"
echo "  journalctl --user-unit $UNIT_NAME.service -f"
if [[ "$USE_TMUX" == "true" ]]; then
  echo "  Attach to session: tmux attach -t $TMUX_SESSION"
fi
