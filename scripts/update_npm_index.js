#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const ora = require('ora');
const spinner = ora('').start();
const registry = require('package-stream');

const dir = '../data/js/';
const name = 'names.json';
const fullDir = `${dir}${name}`;
const filename = path.join(__dirname, fullDir);

let packageCounter = 0;

try {
    if (fs.existsSync(dir)) fs.rmdir(dir);
    else registry()
        .on('package', addPackage)
        .on('up-to-date', done);
} catch (e) {
    done();
    console.log(e.message)
}

function extractVersion(pkg) {
    const latestVersion = (versionsList) => {
        if (Array.isArray(versionsList))
            return Math.max(...versionsList);
        return Math.max(Array.from(Object.keys(versionsList)))
    };
    if (!pkg.versions && !pkg.version)
        throw new Error("Undefined version");
    return pkg.version || latestVersion(pkg.versions);
}

function addPackage(pkg) {
    const packageName = pkg.name;
    if (packageName && packageName.length) {
        const pack = JSON.stringify({
            name: packageName,
            version: extractVersion(pkg)
        }, null, 2);
        packageCounter++;
        if (fs.existsSync(dir))
            fs.appendFileSync(filename, `,\n${pack}`);
        else {
            fs.mkdirSync(dir);
            fs.writeFileSync(filename, `[${pack}`);
        }
    }
}

function done() {
    fs.writeFileSync(filename, `]`);
    process.exit();
}

setInterval(() => {
    spinner.text = `${packageCounter} names collected`
}, 50);
