#!/usr/bin/env node

function extractVersion(pkg) {

    const name = pkg.name;
    let version = null;
    let versions = null;

    if (pkg.version) {
        version = pkg.version
    } else {
        if (pkg.versions instanceof Map) {
            // console.log("Map")
            versions = Object.keys(pkg.versions)
        } else if (pkg.versions instanceof Array) {
            // console.log("Array")
            versions = pkg.versions
        } else {
            console.log(name, "UNDEFINED", pkg)
        }
        if ((versions[versions.length - 1]) && (versions[versions.length - 1].number)) {
            version = versions[versions.length - 1].number
        } else {
            console.log("VERSION UNDEFINED:");
            console.log(pkg)
        }
    }
    return version
}

const fs = require('fs');
const path = require('path');
const names = Array();
const chain = require('lodash').chain;
const isNumber = require('is-number');
const ora = require('ora');
const spinner = ora('').start();
const registry = require('package-stream')({
    db: 'https://replicate.npmjs.com',
    include_docs: true
});

registry
    .on('package', addPackage)
    .on('up-to-date', finish);

function addPackage(pkg) {
    const name = pkg.name;
    const version = extractVersion(pkg);
    if (name && name.length) {
        names.push({
            "name": name,
            "version": version
        })
    }
    // to check the finish function early...
    if (names.length > 100) finish()
}

function finish() {
    const filename = path.join(__dirname, '../data/js/names.json');
    const finalNames = chain(names)
        .compact()
        .uniq()
        .value()
        .filter(name => !isNumber(name));

    fs.writeFileSync(filename, JSON.stringify(finalNames, null, 2));
    console.log(`\nwrote ${finalNames.length} package names to ${filename}`);
    process.exit()
}

setInterval(() => {
    spinner.text = `${names.length} names collected`
}, 50);

