#!/bin/sh
#
### BEGIN INIT INFO
# Provides:          Kurento Room Demo Server
# Required-Start:    $remote_fs $network
# Required-Stop:     $remote_fs
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Kurento Room Demo Server (KROOMDEMO) daemon.
# Description:       KROOMDEMO is a demo app of the Room API
# processname:       kroomdemo
### END INIT INFO

if [ -r "/lib/lsb/init-functions" ]; then
  . /lib/lsb/init-functions
else
  echo "E: /lib/lsb/init-functions not found, package lsb-base needed"
  exit 1
fi

SERVICE_NAME=kroomdemo

[ -z "$DAEMON_USER" ] && DAEMON_USER=nobody

# Find out local or system installation
KROOMDEMO_DIR=$(cd $(dirname $(dirname $0)); pwd)
if [ -f $KROOMDEMO_DIR/bin/start.sh -a -f $KROOMDEMO_DIR/lib/kroomdemo.jar ]; then
    KROOMDEMO_SCRIPT=$KROOMDEMO_DIR/bin/start.sh
    CONSOLE_LOG=$KROOMDEMO_DIR/logs/kroomdemo.log
    KROOMDEMO_CONFIG=$KROOMDEMO_DIR/config/kroomdemo.conf.json
    PIDFILE=$KROOMDEMO_DIR/kroomdemo.pid
else
    # Only root can start Kurento in system mode
    if [ `id -u` -ne 0 ]; then
        log_failure_msg "Only root can start Kurento Room Demo Server"
        exit 1
    fi
    [ -f /etc/default/kroomdemo ] && . /etc/default/kroomdemo
    KROOMDEMO_SCRIPT=/usr/bin/kroomdemo
    CONSOLE_LOG=/var/log/kurento-media-server/kroomdemo.log
    KROOMDEMO_CONFIG=/etc/kurento/kroomdemo.conf.json
    KROOMDEMO_CHUID="--chuid $DAEMON_USER"
    PIDFILE=/var/run/kurento/kroomdemo.pid
fi

# Check startup file
if [ ! -x $KROOMDEMO_SCRIPT ]; then
    log_failure_msg "$KROOMDEMO_SCRIPT is not an executable!"
    exit 1
fi

# Check config file
if [ ! -f $KROOMDEMO_CONFIG ]; then
    log_failure_msg "Kurento Room Demo Server configuration file not found: $KROOMDEMO_CONFIG"
    exit 1;
fi

# Check log directory
[ -d $(dirname $CONSOLE_LOG) ] || mkdir -p $(dirname $CONSOLE_LOG)

start() {
	log_daemon_msg "$SERVICE_NAME starting"
    # clean PIDFILE before start
    if [ -f "$PIDFILE" ]; then
        if [ -n "$(ps h --pid $(cat $PIDFILE) | awk '{print $1}')" ]; then
            log_action_msg "$SERVICE_NAME is already running ..."
            return
        fi
        rm -f $PIDFILE
    fi
    
    # KROOMDEMO instances not identified => Kill them all
    CURRENT_KROOMDEMO=$(ps -ef|grep kroomdemo.jar |grep -v grep | awk '{print $2}')
    [ -n "$CURRENT_KROOMDEMO" ] && kill -9 $CURRENT_KROOMDEMO > /dev/null 2>&1
    
    mkdir -p $(dirname $PIDFILE)
    mkdir -p $(dirname $CONSOLE_LOG)
	[ -f $CONSOLE_LOG ] || touch $CONSOLE_LOG; chown $DAEMON_USER $CONSOLE_LOG
	
	# Start daemon
	start-stop-daemon --start $KROOMDEMO_CHUID \
	    --make-pidfile --pidfile $PIDFILE \
	    --background --no-close \
	    --exec "$KROOMDEMO_SCRIPT" -- >> $CONSOLE_LOG 2>&1
	log_end_msg $?
}

stop () {
	if [ -f $PIDFILE ]; then
	    read kpid < $PIDFILE
	    kwait=15

	    count=0
	    log_daemon_msg "$SERVICE_NAME stopping ..."
	    kill -15 $kpid
	    until [ `ps --pid $kpid 2> /dev/null | grep -c $kpid 2> /dev/null` -eq '0' ] || [ $count -gt $kwait ]
	    do
		sleep 1
		count=$((count+1))
	    done

	    if [ $count -gt $kwait ]; then
		kill -9 $kpid
	    fi

	    rm -f $PIDFILE
	    log_end_msg $?
	else
	    log_failure_msg "$SERVICE_NAME is not running ..."
	fi
}


status() {
	if [ -f $PIDFILE ]; then
	    read ppid < $PIDFILE
	    if [ `ps --pid $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]; then
		log_daemon_msg "$prog is running (pid $ppid)"
		return 0
	    else
		log_daemon_msg "$prog dead but pid file exists"
		return 1
	    fi
	fi
	log_daemon_msg "$SERVICE_NAME is not running"
	return 3
}

case "$1" in
	start)
	    start
	    ;;
	stop)
	    stop
	    ;;
	restart)
	    $0 stop
	    $0 start
	    ;;
	status)
	    status
	    ;;
	*)
	    ## If no parameters are given, print which are avaiable.
	    log_daemon_msg "Usage: $0 {start|stop|status|restart|reload}"
	    ;;
esac
