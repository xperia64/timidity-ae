#!/bin/sh

mv INSTALL INSTALL.autogen.bak
autoreconf -f -i
mv INSTALL.autogen.bak INSTALL

./configure "$@"
