#!/bin/python 

import os

PACKAGES = { "haskell": "../../data/packages/"
           , "rust": "../../data/rust/packages/"
           , "javascript": "../../data/js/packages/"
           }

def stat_by_lang(lang):
    print("extensions for", lang, ":")
    cnt = dict()
    for root, dirs, files in os.walk(PACKAGES[lang]):
        for f in files:
            path, ext = os.path.splitext(f)
            cnt[ext] = cnt.get(ext, 0) + 1

    for ext, amount in list(sorted(list(cnt.iteritems()), key=lambda x: x[1], reverse=True)):
        print("{} ({})".format(ext, amount))

stat_by_lang("rust")

