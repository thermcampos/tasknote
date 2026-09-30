import { InputRule } from '@milkdown/kit/prose/inputrules';
import type { MarkType } from '@milkdown/kit/prose/model';
import { linkSchema } from '@milkdown/kit/preset/commonmark';
import { $inputRule } from '@milkdown/kit/utils';

export const LINK_INPUT_REGEX = /\[(?<text>[^\]]+)\]\((?<href>[^)\s]+)\)$/;

/**
 * Creates a ProseMirror input rule that converts typed markdown link syntax
 * (`[text](url)`) into a link mark as soon as the closing parenthesis is typed.
 *
 * @param {MarkType} linkType - The link mark type from the editor schema.
 * @returns {InputRule} The input rule for markdown links.
 */
export const createLinkInputRule = (linkType: MarkType): InputRule =>
  new InputRule(LINK_INPUT_REGEX, (state, match, start, end) => {
    const text = match.groups?.text;
    const href = match.groups?.href;
    if (!text || !href) return null;
    const tr = state.tr.insertText(text, start, end);
    tr.addMark(start, start + text.length, linkType.create({ href }));
    return tr;
  });

/**
 * Milkdown plugin wiring the markdown link input rule into the editor schema.
 */
export const insertLinkInputRule = $inputRule(ctx => createLinkInputRule(linkSchema.type(ctx)));
