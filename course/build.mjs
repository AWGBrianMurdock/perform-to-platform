import { copyFileSync, mkdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, posix } from 'node:path';
import MarkdownIt from 'markdown-it';

const course = import.meta.dirname;
const root = join(course, '..');
const site = join(course, 'site');
const pages = new Map([
  ['course/index.md', { file: 'index.html', nav: 'Course' }],
  ['CURRICULUM.md', { file: 'curriculum.html', nav: 'Curriculum' }],
  ['course/glossary.md', { file: 'glossary.html', nav: 'Glossary' }],
  ['course/instructor-preflight.md', { file: 'instructor-preflight.html', nav: 'Instructor preflight' }],
]);
const lessonSequence = [
  ['modules/00-orientation/README.md', 'module-0.html', 'Module 0'],
  ['modules/01-business-rules/README.md', 'module-1.html', 'Module 1'],
  ['modules/02-domain-modeling/README.md', 'module-2.html', 'Module 2'],
  ['modules/03-build-debug-review/README.md', 'module-3.html', 'Module 3'],
  ['modules/04-jdbc-persistence/README.md', 'module-4.html', 'Module 4'],
  ['modules/04a-plain-java/README.md', 'module-4a.html', 'Module 4A'],
  ['modules/05-spring-boot-workflow/README.md', 'module-5.html', 'Module 5'],
  ['modules/06-credit-http-api/README.md', 'module-6.html', 'Module 6'],
  ['modules/07-idempotent-http-api/README.md', 'module-7.html', 'Module 7'],
  ['modules/08-secure-service/README.md', 'module-8.html', 'Module 8'],
  ['modules/09-reviewed-delivery/README.md', 'module-9.html', 'Module 9'],
  ['modules/10-credit-decision-capstone/README.md', 'module-10.html', 'Module 10'],
];
for (let index = 0; index < lessonSequence.length; index++) {
  const [source, file, label] = lessonSequence[index];
  const previous = lessonSequence[index - 1];
  const next = lessonSequence[index + 1];
  pages.set(source, {
    file,
    previous: previous && [previous[2], previous[0]],
    next: next && [next[2], next[0]],
  });
}

// Raw HTML is used for disclosure blocks in the checked-in lab guides.
const markdown = new MarkdownIt({ html: true });
const escape = markdown.utils.escapeHtml;
markdown.renderer.rules.table_open = (tokens, index) => {
  const headers = [];
  let inHeader = false;
  for (const token of tokens.slice(index + 1)) {
    if (token.type === 'thead_open') inHeader = true;
    else if (token.type === 'thead_close') break;
    else if (inHeader && token.type === 'inline') headers.push(token.content.trim());
  }
  const label = headers.length ? `Table with columns: ${headers.join(', ')}` : 'Scrollable table';
  return `<div class="table-scroll" tabindex="0" role="region" aria-label="${escape(label)}"><table>\n`;
};
markdown.renderer.rules.table_close = () => '</table></div>\n';

function read(source) {
  return readFileSync(join(root, source), 'utf8');
}

function headings(tokens) {
  const used = new Set();
  return tokens.flatMap((token, index) => {
    if (token.type !== 'heading_open') return [];
    const label = tokens[index + 1].content;
    const slug = label.toLowerCase().replace(/[^\p{L}\p{N}_ -]/gu, '').replaceAll(' ', '-');
    let id = slug;
    let suffix = 1;
    while (used.has(id)) id = `${slug}-${suffix++}`;
    used.add(id);
    token.attrSet('id', id);
    return [{ id, label, tag: token.tag }];
  });
}

for (const [source, page] of pages) {
  const tokens = markdown.parse(read(source), {});
  page.headings = headings(tokens);
  if (tokens[0]?.tag !== 'h1') throw new Error(`Start ${source} with a title`);
  page.title = page.headings[0].label;
  page.titleId = page.headings[0].id;
  page.tokens = tokens.slice(3);
}

const references = new Map();
const diagrams = new Map();

function relative(from, to) {
  return posix.relative(posix.dirname(from), to);
}

function fileLink(source, from, fragment = '') {
  const page = pages.get(source);
  if (page) {
    if (fragment && !page.headings.some(({ id }) => id === decodeURIComponent(fragment))) {
      throw new Error(`Unknown section in ${source}#${fragment} (linked from ${from})`);
    }
    const path = from === page.file && fragment ? '' : relative(from, page.file);
    return path + (fragment ? `#${fragment}` : '');
  }

  if (source.startsWith('../') || posix.isAbsolute(source) || !statSync(join(root, source), { throwIfNoEntry: false })?.isFile()) {
    throw new Error(`Unknown course file ${source} (linked from ${from})`);
  }

  if (source.endsWith('.md')) {
    throw new Error(`Markdown page is not included in the course site: ${source} (linked from ${from})`);
  }

  if (source.startsWith('course/diagrams/') && source.endsWith('.svg') && !fragment) {
    const target = posix.relative('course', source);
    diagrams.set(source, target);
    return relative(from, target);
  }

  if (!/\.(java|cob|xml)$/.test(source) || fragment) {
    throw new Error(`Unsupported source reference ${source} (linked from ${from})`);
  }
  const target = `reference/${source}.html`;
  references.set(source, target);
  return relative(from, target);
}

function lessonContent(source, page) {
  for (const { children } of page.tokens) {
    for (const token of children ?? []) {
      const attribute = { link_open: 'href', image: 'src' }[token.type];
      if (!attribute) continue;
      const href = token.attrGet(attribute);
      if (/^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(href)) continue;
      const [path, fragment] = href.split('#', 2);
      const target = path ? posix.join(posix.dirname(source), decodeURIComponent(path)) : source;
      token.attrSet(attribute, fileLink(target, page.file, fragment));
    }
  }
  return markdown.renderer.render(page.tokens, markdown.options, {});
}

function renderPage(page, content, beforeTitle = '') {
  const nav = [...pages.values()].filter(({ nav }) => nav).map(({ file, nav }) => {
    const current = file === page.file ? ' aria-current="page"' : '';
    return `<a href="${relative(page.file, file)}"${current}>${escape(nav)}</a>`;
  }).join('\n');
  const sections = page.headings.filter(({ tag }) => tag === 'h2');
  const contents = sections.length ? `<nav class="contents" aria-label="On this page">
    <p>On this page</p>
    <ul>${sections.map(({ id, label }) => `<li><a href="#${escape(id)}">${escape(label)}</a></li>`).join('')}</ul>
  </nav>` : '';
  const previous = page.previous && `<a rel="prev" href="${fileLink(page.previous[1], page.file)}">← ${page.previous[0]}</a>`;
  const next = page.next && `<a rel="next" href="${fileLink(page.next[1], page.file)}">${page.next[0]} →</a>`;
  const lessons = previous || next
    ? `<nav class="lessons" aria-label="Lab sequence">${previous ?? ''}${next ?? ''}</nav>`
    : '';
  const title = page.file === 'index.html' ? page.title : `${page.title} | Perform to Platform`;
  const titleId = page.titleId ? ` id="${escape(page.titleId)}"` : '';

  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escape(title)}</title>
  <link rel="stylesheet" href="${relative(page.file, 'course.css')}">
</head>
<body>
  <a class="skip" href="#main">Skip to content</a>
  <header class="site-header">
    <div class="header-inner">
      <a class="wordmark" href="${relative(page.file, 'index.html')}">Perform to Platform</a>
      <nav aria-label="Course resources">${nav}</nav>
    </div>
  </header>
  <main id="main" class="layout${contents ? ' has-contents' : ''}" tabindex="-1">
    <div class="page-title">
      ${lessons}${beforeTitle}
      <h1${titleId}>${escape(page.title)}</h1>
    </div>
    ${contents}
    <div class="page-body">
      ${content}
    </div>
  </main>
</body>
</html>
`;
}

const output = new Map();
for (const [source, page] of pages) {
  output.set(page.file, renderPage(page, lessonContent(source, page)));
}

for (const [source, file] of references) {
  const title = posix.basename(source);
  const lab = pages.get(`${source.split('/').slice(0, 2).join('/')}/README.md`);
  const back = lab ? `<p><a href="${relative(file, lab.file)}">← ${escape(lab.title)}</a></p>` : '';
  const content = `<p class="source-path"><code>${escape(source)}</code></p>
<pre class="source-code" tabindex="0"><code>${escape(read(source))}</code></pre>`;
  output.set(file, renderPage({ file, title, headings: [] }, content, back));
}

rmSync(site, { recursive: true, force: true });
for (const [file, html] of output) {
  const destination = join(site, file);
  mkdirSync(dirname(destination), { recursive: true });
  writeFileSync(destination, html);
}
copyFileSync(join(course, 'course.css'), join(site, 'course.css'));
for (const [source, file] of diagrams) {
  const destination = join(site, file);
  mkdirSync(dirname(destination), { recursive: true });
  copyFileSync(join(root, source), destination);
}
console.log(`Built ${pages.size} course pages and ${references.size} source references in course/site.`);
