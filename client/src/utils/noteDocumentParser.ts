const TAGS_FOOTER_PATTERN = /^(tags:\s*)(.*)$/i;
const URL_LINE_PATTERN = /^url:\s*(.*)$/i;

export interface ParsedNoteDocument {
  title: string;
  url: string;
  body: string;
  tags: string[];
}

const normalizeTitleLine = (line: string): string => line.replace(/^#+\s+/, '').trim();

/**
 * Parses a legacy note document (title line, `url:` line, `tags:` footer) into
 * separate fields. Only used to migrate localStorage drafts created before the
 * hybrid note form existed.
 *
 * @param {string} content - The legacy note document.
 * @returns {ParsedNoteDocument} The parsed title, url, body and tags.
 */
export const parseNoteDocument = (content: string): ParsedNoteDocument => {
  const lines = content.split('\n');
  const title = lines.length > 0 ? normalizeTitleLine(lines[0].replace(/\r$/, '')) : '';

  let footerLineIndex = lines.length;
  let tags: string[] = [];
  for (let i = lines.length - 1; i >= 0; i -= 1) {
    const line = lines[i].replace(/\r$/, '');
    if (line.trim() === '') continue;
    const match = line.match(TAGS_FOOTER_PATTERN);
    if (match) {
      footerLineIndex = i;
      tags = match[2]
        .split(',')
        .map(token => token.trim().toLowerCase())
        .filter((token, index, all) => token.length > 0 && all.indexOf(token) === index);
    }
    break;
  }

  let url = '';
  let urlLineIndex = -1;
  for (let i = 1; i < footerLineIndex; i += 1) {
    const match = lines[i].replace(/\r$/, '').match(URL_LINE_PATTERN);
    if (match) {
      urlLineIndex = i;
      url = match[1].split(/[\s,]+/).filter(token => token.length > 0)[0] ?? '';
      break;
    }
  }

  const body = lines
    .slice(1, footerLineIndex)
    .filter((_, index) => index + 1 !== urlLineIndex)
    .join('\n')
    .replace(/^\s+/, '')
    .replace(/\s+$/, '');

  return { title, url, body, tags };
};
