#!/usr/bin/env python3

import unicodedata
import sys
import re
import os
import html
import json
from tqdm import tqdm

def make_identifier(content):
    content = html.unescape(content)
    ident = ""

    for i, c in enumerate(content):
        if ord(c) < 128:
            ident += c
        else:
            ident += unicodedata.name(c)

    ident = ident.lower()
    ident = re.sub("[^a-z0-9_]", "_", ident)
    ident = re.sub("__*", "_", ident)
    if ident.endswith("_"):
        ident = ident[:-1]
    if ident.startswith("_"):
        ident = ident[1:]
    return ident

jsons = []
for i in tqdm(os.listdir("data")):
    with open(os.path.join("data", i), "r") as f:
        j = json.load(f)
        j["id"] = make_identifier(j["name"])
        jsons.append(j)

idcount = {}
for j in tqdm(jsons):
    try:
        idcount[j["id"]] += 1
    except KeyError:
        idcount[j["id"]] = 1

def unescape(s):
    x = html.unescape(s)
    while x != s:
        s = x
        x = html.unescape(s)
    return s.replace('"', '\\"')

output = []
counter = {}
json_out = []
for j in tqdm(jsons):
    try:
        counter[j["id"]] += 1
    except KeyError:
        counter[j["id"]] = 1

    ident = j["id"]
    if idcount[j["id"]] > 1:
        ident = ident + "_" + str(counter[j["id"]])

    name = unescape(j["name"])
    category = make_identifier(j["category"])
    tags = ['"' + unescape(i) + '"' for i in j["categories"]]

    o = {}
    o["id"] = ident
    o["name"] = name
    o["category"] = category
    o["tags"] = tags
    o["texture"] = j["texture"]
    json_out.append(o)

    enum_id = category.upper() + "_" + ident.upper()
    output.append(f'{enum_id},')

for o in sorted(output):
    print(o)

with open("head_library.json", "w") as f:
    json.dump(json_out, f)
