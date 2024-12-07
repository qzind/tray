#!/usr/bin/env python3

import os

def notify(title, message):
  os.system(f"notify-send '{title}' '{message}'")

title=os.getenv('APP_TITLE')
version=os.getenv('APP_VERSION')
printer="\U0001F5A8"
tada="\U0001F389"

notify("{} {}".format(printer, title), """{} This is a sample message from {} {}.

This message indicates that provisioning startup tasks are working.""".format(tada, title, version))