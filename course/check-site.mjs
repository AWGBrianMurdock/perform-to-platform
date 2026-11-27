import assert from 'node:assert/strict';
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { isAbsolute, join, relative, resolve, sep } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const site = resolve(process.argv[2] ?? join(import.meta.dirname, 'site'));
const requiredLinks = new Map([
  ['index.html', ['module-0.html', 'module-1.html', 'curriculum.html', 'glossary.html', 'instructor-preflight.html']],
  ['curriculum.html', [
    'module-0.html', 'module-1.html', 'module-2.html', 'module-3.html', 'module-4.html', 'module-4a.html',
    'module-5.html', 'module-6.html', 'module-7.html', 'module-8.html', 'module-9.html', 'module-10.html',
  ]],
  ['module-0.html', ['diagrams/java-execution.svg', 'glossary.html#breakpoint', 'module-1.html']],
  ['module-1.html', ['diagrams/credit-rule-inputs.svg', 'glossary.html#parameterized-test', 'module-0.html', 'module-2.html']],
  ['module-2.html', ['module-1.html', 'module-3.html']],
  ['module-3.html', ['module-2.html', 'module-4.html']],
  ['module-4.html', ['module-3.html', 'module-4a.html']],
  ['module-4a.html', ['module-4.html', 'module-5.html']],
  ['module-5.html', ['module-4a.html', 'module-6.html']],
  ['module-6.html', ['module-5.html', 'module-7.html']],
  ['module-7.html', ['module-6.html', 'module-8.html']],
  ['module-8.html', ['module-7.html', 'module-9.html']],
  ['module-9.html', ['module-8.html', 'module-10.html']],
  ['module-10.html', ['module-9.html']],
  ['glossary.html', ['module-0.html#trace-execution-in-the-debugger']],
  ['instructor-preflight.html', [
    'module-0.html#run-the-jar',
    'module-1.html#run-the-jar',
    'module-1.html#add-a-maximum-request-rule',
  ]],
]);
const pages = new Map();
const requiredPages = [
  'index.html', 'curriculum.html', 'glossary.html', 'instructor-preflight.html',
  'module-0.html', 'module-1.html', 'module-2.html', 'module-3.html', 'module-4.html', 'module-4a.html',
  'module-5.html', 'module-6.html', 'module-7.html', 'module-8.html', 'module-9.html', 'module-10.html',
];

for (const file of readdirSync(site, { recursive: true })) {
  if (!file.endsWith('.html')) continue;
  const html = readFileSync(join(site, file), 'utf8');
  const ids = [...html.matchAll(/<[a-z][^>]*?\sid="([^"]+)"/gi)].map((match) => match[1]);
  assert.match(html, /<html lang="en">/, `Missing document language in ${file}`);
  assert.equal([...html.matchAll(/<h1\b/gi)].length, 1, `Expected one page title in ${file}`);
  assert.match(html, /<main id="main"(?:\s|>)/, `Missing main content in ${file}`);
  assert.doesNotMatch(html, /href="https:\/\/github\.com\/bmurdock\/perform-to-platform\//,
    `Internal course links must stay within the packaged site in ${file}`);
  for (const [, image] of html.matchAll(/<img\b([^>]*)>/gi)) {
    assert.match(image, /\salt="[^"]+"/i, `Image is missing descriptive alt text in ${file}`);
  }
  for (const [, region] of html.matchAll(/<div\b([^>]*\brole="region"[^>]*)>/gi)) {
    assert.match(region, /\baria-label="[^"]+"/i, `Scrollable region is missing a name in ${file}`);
  }
  assert.equal(new Set(ids).size, ids.length, `Duplicate anchors in ${file}`);
  pages.set(file, { html, ids: new Set(ids) });
}

for (const file of requiredPages) {
  assert.ok(pages.has(file), `Missing course page ${file}`);
}

let checked = 0;
for (const [file, { html }] of pages) {
  const pageUrl = pathToFileURL(join(site, file));
  const addresses = new Set();
  // The generator emits quoted attributes and escapes literal HTML in source examples.
  for (const [, address] of html.matchAll(/<(?:a|img|link)\b[^>]*?\s(?:href|src)="([^"]+)"/gi)) {
    const detail = `${file} references ${address}`;
    const url = new URL(address, pageUrl);
    if (url.protocol === 'https:' || url.protocol === 'http:') continue;
    addresses.add(address);
    assert.equal(url.protocol, 'file:', `Unsupported URL in ${detail}`);
    const target = fileURLToPath(url);
    const targetFile = relative(site, target);
    assert.ok(targetFile && !isAbsolute(targetFile) && !targetFile.startsWith(`..${sep}`),
      `Link leaves the archive: ${detail}`);
    assert.ok(statSync(target, { throwIfNoEntry: false })?.isFile(), `Missing file: ${detail}`);
    if (url.hash && targetFile.endsWith('.html')) {
      assert.ok(pages.get(targetFile)?.ids.has(decodeURIComponent(url.hash.slice(1))),
        `Missing anchor: ${detail}`);
    }
    checked++;
  }
  for (const required of requiredLinks.get(file) ?? []) {
    assert.ok(addresses.has(required), `Missing pilot link in ${file}: ${required}`);
  }
}

console.log(`Verified ${pages.size} generated pages and ${checked} local links and assets.`);
