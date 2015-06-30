#!/bin/sh

DIRNAME=$(dirname "$0")
GREP="grep"

# OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
linux=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;

    Linux)
        linux=true
        ;;
esac

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

if [ "$PRESERVE_JAVA_OPTS" != "true" ]; then
    # Check for -d32/-d64 in JAVA_OPTS
    JVM_D64_OPTION=`echo $JAVA_OPTS | $GREP "\-d64"`
    JVM_D32_OPTION=`echo $JAVA_OPTS | $GREP "\-d32"`

    # Check If server or client is specified
    SERVER_SET=`echo $JAVA_OPTS | $GREP "\-server"`
    CLIENT_SET=`echo $JAVA_OPTS | $GREP "\-client"`

    if [ "x$JVM_D32_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d32"
    elif [ "x$JVM_D64_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d64"
    elif $darwin && [ "x$SERVER_SET" = "x" ]; then
        # Use 32-bit on Mac, unless server has been specified or the user opts are incompatible
        "$JAVA" -d32 $JAVA_OPTS -version > /dev/null 2>&1 && PREPEND_JAVA_OPTS="-d32" && JVM_OPTVERSION="-d32"
    fi

    CLIENT_VM=false
    if [ "x$CLIENT_SET" != "x" ]; then
        CLIENT_VM=true
    elif [ "x$SERVER_SET" = "x" ]; then
        if $darwin && [ "$JVM_OPTVERSION" = "-d32" ]; then
            # Prefer client for Macs, since they are primarily used for development
            CLIENT_VM=true
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -client"
        else
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -server"
        fi
    fi

    if [ $CLIENT_VM = false ]; then
        NO_COMPRESSED_OOPS=`echo $JAVA_OPTS | $GREP "\-XX:\-UseCompressedOops"`
        if [ "x$NO_COMPRESSED_OOPS" = "x" ]; then
            "$JAVA" $JVM_OPTVERSION -server -XX:+UseCompressedOops -version >/dev/null 2>&1 && PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -XX:+UseCompressedOops"
        fi

        NO_TIERED_COMPILATION=`echo $JAVA_OPTS | $GREP "\-XX:\-TieredCompilation"`
        if [ "x$NO_TIERED_COMPILATION" = "x" ]; then
            "$JAVA" $JVM_OPTVERSION -server -XX:+TieredCompilation -version >/dev/null 2>&1 && PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -XX:+TieredCompilation"
        fi
    fi

    JAVA_OPTS="$PREPEND_JAVA_OPTS $JAVA_OPTS"
fi

# Find out installation type
KROOMDEMO_HOME=$(cd $DIRNAME/..;pwd)
KROOMDEMO_BINARY=$KROOMDEMO_HOME/lib/kroomdemo.jar
if [ ! -f $KROOMDEMO_BINARY ]; then
    # System installation
    [ -f /etc/default/kroomdemo ] && . /etc/default/kroomdemo
    [ -f /etc/sysconfig/kroomdemo ] && . /etc/sysconfig/kroomdemo
    KROOMDEMO_HOME=/var/lib/kurento
    KROOMDEMO_BINARY=$KROOMDEMO_HOME/kroomdemo.jar
    KROOMDEMO_CONFIG="/etc/kurento/kroomdemo.conf.json"
    KROOMDEMO_LOG_CONFIG=/etc/kurento/kroomdemo-log4j.properties
    KROOMDEMO_LOG_FILE=/var/log/kurento-media-server/kroomdemo.log
else
    # Home based installation
    KROOMDEMO_CONFIG=$KROOMDEMO_HOME/config/kroomdemo.conf.json
    KROOMDEMO_LOG_CONFIG=$KROOMDEMO_HOME/config/kroomdemo-log4j.properties
    KROOMDEMO_LOG_FILE=$KROOMDEMO_HOME/logs/kroomdemo.log
    mkdir -p $KROOMDEMO_HOME/logs
fi

# logging.config ==> Springboot logging config
# log4j.configuration ==> log4j default config. Do not remove to avoid exception for all login taking place before Springboot has started
KROOMDEMO_OPTS="$KROOMDEMO_OPTS -DconfigFilePath=$KROOMDEMO_CONFIG"
KROOMDEMO_OPTS="$KROOMDEMO_OPTS -Dkroomdemo.log.file=$KROOMDEMO_LOG_FILE"
KROOMDEMO_OPTS="$KROOMDEMO_OPTS -Dlogging.config=$KROOMDEMO_LOG_CONFIG"
KROOMDEMO_OPTS="$KROOMDEMO_OPTS -Dlog4j.configuration=file:$KROOMDEMO_LOG_CONFIG"

[ -f $KROOMDEMO_CONFIG ] || { echo "Unable to find configuration file: $KROOMDEMO_CONFIG"; exit 1; }

# Display our environment
echo "========================================================================="
echo ""
echo "  Kurento Room Demo Server Bootstrap Environment"
echo ""
echo "  KROOMDEMO_BINARY: $KROOMDEMO_BINARY"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "  KROOMDEMO_OPTS: $KROOMDEMO_OPTS"
echo ""
echo "========================================================================="
echo ""

cd $KROOMDEMO_HOME
exec $JAVA $JAVA_OPTS $KROOMDEMO_OPTS -jar $KROOMDEMO_BINARY 
