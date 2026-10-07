// Flow tests for the ported JetBrains actions (messages and severities must match the JetBrains plugin).
import { describe, expect, it } from 'vitest';
import {
  convertClipboardToXml,
  getFileMakerCalculationFromClipboard,
  getFileMakerClipboardContent,
  isXmlFileExtension,
  pushClipboardIntoFileMaker,
  readClipboardIntoNewXmlFile,
} from '../src/actions/actions';
import { ClipboardAccessException } from '../src/clipboard/ClipboardService';
import { fakeContext, target } from './fakes';

const STEPS = '<fmxmlsnippet type="FMObjectList"><Step enable="True" id="1" name="Beep"/></fmxmlsnippet>';

describe('Get FileMaker Clipboard Content (smart action)', () => {
  it('saves an fmxmlsnippet to a new .xml file and puts the XML on the clipboard', async () => {
    const { ctx, files, clipboard, notifier } = fakeContext({ clipboard: `junk ${STEPS} junk` });
    await getFileMakerClipboardContent(ctx);
    expect(files.created).toEqual([expect.objectContaining({ extension: '.xml', content: STEPS })]);
    expect(clipboard.writes).toEqual([STEPS]);
    expect(notifier.all.map((n) => n.message)).toEqual([
      'Success: Wrote XML to file: .fmCuttingBoard/1.xml',
      'Success: Saved XML to file and replaced clipboard with XML.',
    ]);
  });

  it('saves anything else as a .fmcalc file without touching the clipboard', async () => {
    const { ctx, files, clipboard, notifier } = fakeContext({ clipboard: 'If ( a ; b ; c )' });
    await getFileMakerClipboardContent(ctx);
    expect(files.created).toEqual([expect.objectContaining({ extension: '.fmcalc', content: 'If ( a ; b ; c )' })]);
    expect(clipboard.writes).toEqual([]);
    expect(notifier.last).toMatchObject({ level: 'info', message: 'Created: .fmCuttingBoard/1.fmcalc' });
  });

  it('reports an empty clipboard', async () => {
    const { ctx, files, notifier } = fakeContext({ clipboard: '  \n' });
    await getFileMakerClipboardContent(ctx);
    expect(files.created).toEqual([]);
    expect(notifier.last).toMatchObject({ level: 'info', message: 'Clipboard is empty or has no text content.' });
  });

  it('reports a clipboard read failure with details', async () => {
    const { ctx, clipboard, notifier } = fakeContext();
    clipboard.readError = new ClipboardAccessException('busy');
    await getFileMakerClipboardContent(ctx);
    expect(notifier.last).toMatchObject({ level: 'error', message: 'Could not read clipboard: busy', hasDetails: true });
  });

  it('previews before replacing the clipboard, and cancels cleanly', async () => {
    const { ctx, clipboard, preview, notifier, files } = fakeContext({
      clipboard: STEPS,
      settings: { previewBeforeClipboardWrite: true },
      previewAnswer: false,
    });
    await getFileMakerClipboardContent(ctx);
    expect(preview.shown).toEqual([{ title: 'Preview: Replace Clipboard With XML', content: STEPS, limit: 800 }]);
    expect(files.created).toHaveLength(1);
    expect(clipboard.writes).toEqual([]);
    expect(notifier.last).toMatchObject({ level: 'info', message: 'Canceled: Clipboard was not modified.' });
  });

  it('still reports the file when writing the clipboard fails', async () => {
    const { ctx, clipboard, notifier } = fakeContext({ clipboard: STEPS });
    clipboard.writeError = new ClipboardAccessException('denied');
    await getFileMakerClipboardContent(ctx);
    expect(notifier.last).toMatchObject({
      level: 'error',
      message: 'Saved XML to file, but failed to write XML to clipboard: denied',
    });
  });
});

describe('New XML File From FM Clipboard', () => {
  it('warns when the clipboard holds no fmxmlsnippet', async () => {
    const { ctx, files, notifier } = fakeContext({ clipboard: 'plain text' });
    await readClipboardIntoNewXmlFile(ctx);
    expect(files.created).toEqual([]);
    expect(notifier.last).toMatchObject({
      level: 'warn',
      message: 'Clipboard does not contain recognizable FileMaker content or fmxmlsnippet.',
    });
  });

  it('reports file creation failures', async () => {
    const { ctx, files, notifier } = fakeContext({ clipboard: STEPS });
    files.error = new Error('No folder is open. Open a folder or workspace first.');
    await readClipboardIntoNewXmlFile(ctx);
    expect(notifier.last).toMatchObject({
      level: 'error',
      message: 'Failed to create/write XML file: No folder is open. Open a folder or workspace first.',
    });
  });
});

describe('Get FileMaker Calculation From Clipboard', () => {
  it('reports an empty clipboard', async () => {
    const { ctx, notifier } = fakeContext({ clipboard: '' });
    await getFileMakerCalculationFromClipboard(ctx);
    expect(notifier.last).toMatchObject({ level: 'info', message: 'Clipboard is empty or does not contain text to save.' });
  });
});

describe('Convert FM Clipboard To XML Clipboard', () => {
  it('replaces the clipboard with the normalized XML', async () => {
    const { ctx, clipboard, notifier } = fakeContext({ clipboard: `\n${STEPS}\n` });
    await convertClipboardToXml(ctx);
    expect(clipboard.writes).toEqual([STEPS]);
    expect(notifier.last?.message).toBe(
      'Success: Converted FileMaker clipboard content to XML and placed it on the clipboard.',
    );
  });
});

describe('Push Clipboard Into FileMaker', () => {
  it('only accepts .xml files (case-insensitive)', () => {
    expect(isXmlFileExtension('xml')).toBe(true);
    expect(isXmlFileExtension('XML')).toBe(true);
    expect(isXmlFileExtension('fmcalc')).toBe(false);
    expect(isXmlFileExtension(undefined)).toBe(false);
  });

  it('asks for an XML file when none is focused', async () => {
    const { ctx, notifier } = fakeContext();
    await pushClipboardIntoFileMaker(ctx, target('txt', STEPS));
    expect(notifier.last).toMatchObject({ level: 'warn', message: 'Please focus an XML file to push into FileMaker.' });
  });

  it('reports an empty file', async () => {
    const { ctx, notifier } = fakeContext();
    await pushClipboardIntoFileMaker(ctx, target('xml', '   '));
    expect(notifier.last).toMatchObject({ level: 'info', message: 'The active XML file is empty.' });
  });

  it('rejects XML that is not a supported fmxmlsnippet', async () => {
    const { ctx, clipboard, notifier } = fakeContext();
    await pushClipboardIntoFileMaker(ctx, target('xml', '<root><Step/></root>'));
    expect(clipboard.writes).toEqual([]);
    expect(notifier.last).toMatchObject({ level: 'warn', message: 'The file does not contain a supported fmxmlsnippet.' });
  });

  it('writes the validated, trimmed XML to the clipboard', async () => {
    const { ctx, clipboard, notifier } = fakeContext();
    await pushClipboardIntoFileMaker(ctx, target('xml', `\n${STEPS}\n`));
    expect(clipboard.writes).toEqual([STEPS]);
    expect(notifier.last).toMatchObject({
      level: 'info',
      message: 'Success: Converted XML and placed FileMaker-compatible content on the clipboard.',
    });
  });

  it('honours the preview setting', async () => {
    const { ctx, clipboard, preview, notifier } = fakeContext({ settings: { previewBeforeClipboardWrite: true }, previewAnswer: false });
    await pushClipboardIntoFileMaker(ctx, target('xml', STEPS));
    expect(preview.shown[0].title).toBe('Preview: Push Clipboard Into FileMaker');
    expect(clipboard.writes).toEqual([]);
    expect(notifier.last?.message).toBe('Canceled: No changes were made to the clipboard.');
  });
});
