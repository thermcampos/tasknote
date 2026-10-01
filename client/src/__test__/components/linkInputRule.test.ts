import { describe, expect, it } from 'vitest';
import { Schema } from '@milkdown/kit/prose/model';
import { EditorState } from '@milkdown/kit/prose/state';
import { LINK_INPUT_REGEX, createLinkInputRule } from '../../components/MarkdownEditor/linkInputRule';

const schema = new Schema({
  nodes: {
    doc: { content: 'paragraph+' },
    paragraph: { content: 'text*', group: 'block' },
    text: {}
  },
  marks: {
    link: {
      attrs: { href: { default: '' }, title: { default: null } },
      toDOM: mark => ['a', mark.attrs, 0],
      parseDOM: [{ tag: 'a[href]' }]
    }
  }
});

const buildState = (typed: string) => {
  const state = EditorState.create({ schema });
  return state.apply(state.tr.insertText(typed));
};

describe('createLinkInputRule', () => {
  it('should convert typed markdown link syntax into a link mark', () => {
    const typed = '[text](https://my-link.com)';
    const state = buildState(typed);
    const match = typed.match(LINK_INPUT_REGEX);
    expect(match).not.toBeNull();

    const rule = createLinkInputRule(schema.marks.link);
    const tr = rule.handler(state, match!, 1, 1 + typed.length);
    expect(tr).not.toBeNull();

    const result = state.apply(tr!);
    const textNode = result.doc.firstChild!.firstChild!;
    expect(textNode.text).toBe('text');
    const link = textNode.marks.find(mark => mark.type === schema.marks.link);
    expect(link?.attrs.href).toBe('https://my-link.com');
  });

  it('should match only the link suffix of the text before the cursor', () => {
    const typed = 'see [docs](https://example.com/docs)';
    const match = typed.match(LINK_INPUT_REGEX);
    expect(match?.groups?.text).toBe('docs');
    expect(match?.groups?.href).toBe('https://example.com/docs');
  });

  it('should not match plain text or incomplete link syntax', () => {
    expect('just some text'.match(LINK_INPUT_REGEX)).toBeNull();
    expect('[text](https://my-link.com'.match(LINK_INPUT_REGEX)).toBeNull();
    expect('[text] (https://my-link.com)'.match(LINK_INPUT_REGEX)).toBeNull();
  });
});
