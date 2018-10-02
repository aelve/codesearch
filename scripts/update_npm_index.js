#!/usr/bin/env node

const fs = require('fs-extra');
const path = require('path');
const ora = require('ora');
const spinner = ora('').start();
const registry = require('package-stream');

const dir = '../data/js/';
const name = 'names.json';
const fullDir = `${dir}${name}`;
const filename = path.join(__dirname, fullDir);

let packageCounter = 0;

start();

function start() {
    if (fs.existsSync(dir))
        fs.remove(dir, updatePackages);
    else updatePackages();
}

function updatePackages() {
    registry()
        .on('package', addPackage)
        .on('up-to-date', done);
}

function extractVersion(pkg) {
    const latestVersion = (versionsList) => {
        if (Array.isArray(versionsList))
            return Math.max(...versionsList);
        return Math.max(Array.from(null.keys()))
    };
    if (!pkg.versions && !pkg.version)
        throw new Error("Undefined version");
    return pkg.version || latestVersion(pkg.versions);
}

function addPackage(pkg) {
    const packageName = pkg.name;
    if (packageName && packageName.length) {
        try {
            const pack = JSON.stringify({
                name: packageName,
                version: extractVersion(pkg)
            }, null, 2);
            if (fs.existsSync(dir))
                fs.appendFileSync(filename, `,\n${pack}`);
            else {
                fs.mkdirSync(dir);
                fs.writeFileSync(filename, `[${pack}`);
            }
            packageCounter++
        } catch (e) {
            console.log(e.message)
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
