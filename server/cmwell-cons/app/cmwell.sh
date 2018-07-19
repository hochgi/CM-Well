#!/bin/sh


# CM-WELL SPLASH SCREEN
echo -e '\n                         Welcome to CM-Well Console\n                                                                                 \n                                                                                \n                             .-::/++ooooo++//:-.`                               \n                         .:/oooooo+oo+++o++oooooo+/:.`                          \n                      .:+oo+++++o+ooooooo++++++++++ooo/-`                       \n                    ./oo++++oooooooooooo+++++//////:/++oo/-`                    \n                  `/o++++oooooooooooo+++++++////::::::-:/+o+:`                  \n                 :oo+++/:-----::/+o++++++//////::::-----.-:+oo/`                \n               .+ooo+-.....---::-.--:///////:::::----.......-+o+:               \n             `./o+:.````..----://:-   ``.-::::-----......`````:+o+.             \n           `---o+`  . `...----:/::-.       `..---.....```````` ./oo-            \n          ..-`+o-  ````..----://:++/:-.``     ``....``````..``` `:oo:           \n        `:`.`-o+   `.`..--::///::--:://///:-.``  ``` `...----:/:. -oo:          \n       `:. -`/o:    .--:://////:--.`   `.-://+/:-..`.``..----://:-`-oo-         \n      `.- `-`+o-     .:///////::-.`         ``.-://+/:-..----:/::-- :oo.        \n      -.` .  /o.       `-:://:-.`                 ..-/:.----://:---  +o/        \n     ..-  .  /o-          `:+/                    `.`.---:////::-.-  .oo.       \n     :`-  .  -o:           `/o.                    .:////////::---`   +o:       \n    `` .  .  `o+`           -++                   ./o+://////::-.`    /o+       \n    .  .  ..  /o:           `:o:                ./o+-``.--:--.``      -oo       \n    .` -`.`-  `+o`           ./o`             ./o+-`                  -o+       \n     . ..  .`  -o+`           :+/           ./o/-`                    :o/       \n     -  .   .   :o/`          `/o-````    ./o/-`                      +o-       \n     .` .`   .`.`:o/`         `-+o----:-./o/-`                       -o+`       \n      -  .   `-   -++.      ```.:+/---//:/-`                        `+o-        \n      `. `-   `.`  ./o:`   .```.-:---://:--                      ```/o/         \n       `-.`.`  `.` ``:++-` . `...---://::--.                  ````./o/          \n        `.  .`  `..```./o+--`..---:///::-..`               `````.-+o:           \n         `.` ..```.:.```-/+o/::://////::-..               ```..-/o/.            \n           ..``---.`.--...-:+o++/////::--.              ``..-:+o+-              \n            `..`.-.....-:----:/+oo++/:-.               ..-/+o+/.                \n              `-...---.--:::::////++oo+/:-..```````.--/+oo+/-`                  \n                `--.-:::---::////////++++ooooooooooo+o+/:.                      \n                  `.-:-:////:////++++++++++o++ooo+++/-`                         \n                     `.::////+/+++++++ooooooooo++:.                             \n                         `.--:/+++++ooooo++/:-.`                                \n                                 ````````                                       \n                                                                                '


cd $(dirname -- "$0")


export CMWELL_CONS_PATH=$(pwd)

if [ -e "cmw.env" ]
then
  . ./cmw.env
fi

if [ -n "$CMWELL_ENV_DEF_DIR" ]; then
  echo -e "You are working in PRIVATE environment" 
fi

CONS=`echo components/cmwell-cons*`
CTRL=`echo components/cmwell-controller-assembly*`

bash -c "scala -Dscala.color -nc -cp '$CMWELL_CONS_PATH/cons-lib/*' "

