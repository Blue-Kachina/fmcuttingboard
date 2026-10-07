// 1:1 ports of the JetBrains actions in jetbrains/src/main/java/dev/fmcuttingboard/actions/. Titles, messages and
// notification severities match the JetBrains plugin so both behave the same; keep them in step.
import { ClipboardAccessException } from '../clipboard/ClipboardService';
import { messageOf } from '../clipboard/DefaultClipboardService';
import { ClipboardToXmlConverter } from '../core/ClipboardToXmlConverter';
import { ConversionException } from '../core/ConversionException';
import { DefaultXmlToClipboardConverter } from '../core/DefaultXmlToClipboardConverter';
import { javaIsBlank } from '../core/javaCompat';
import { PREVIEW_LIMIT, type ActionContext, type PushTarget } from './ports';

const converter = new ClipboardToXmlConverter();
const xmlToClipboard = new DefaultXmlToClipboardConverter();

/** Reads the clipboard, reporting failures under `title`. Returns undefined when the action should stop. */
async function readClipboard(ctx: ActionContext, title: string): Promise<string | undefined> {
  try {
    return await ctx.clipboard.readText();
  } catch (ex) {
    ctx.notifier.error(title, `Could not read clipboard: ${messageOf(ex)}`, ex);
    return undefined;
  }
}

/** Optional preview; true to proceed. Preview failures never block the write (as in JetBrains). */
async function confirmIfPreviewing(ctx: ActionContext, previewTitle: string, content: string): Promise<boolean> {
  try {
    if (ctx.settings().previewBeforeClipboardWrite) {
      return await ctx.preview.confirmWrite(previewTitle, content, PREVIEW_LIMIT);
    }
  } catch (ex) {
    ctx.log.warn(`Preview failed; proceeding with clipboard write: ${messageOf(ex)}`);
  }
  return true;
}

// ----- ReadClipboardIntoNewXmlFileAction ("New XML File From FM Clipboard") -----

export async function readClipboardIntoNewXmlFile(ctx: ActionContext, clipboardText?: string): Promise<void> {
  const title = 'New XML File From FM Clipboard';
  ctx.log.info('Invoke: ReadClipboardIntoNewXmlFileAction');

  const text = clipboardText ?? (await readClipboard(ctx, title));
  if (text === undefined) return;
  if (javaIsBlank(text)) {
    ctx.notifier.info(title, 'Clipboard is empty or contains no text to save.');
    return;
  }

  let xml: string;
  try {
    xml = converter.convertToXml(text);
  } catch (ex) {
    if (ex instanceof ConversionException) {
      ctx.notifier.warn(title, 'Clipboard does not contain recognizable FileMaker content or fmxmlsnippet.');
    } else {
      ctx.notifier.error(title, `Unexpected error during conversion: ${messageOf(ex)}`, ex);
    }
    return;
  }

  try {
    const file = await ctx.files.createAndOpen(ctx.settings(), '.xml', xml);
    ctx.notifier.info(title, `Success: Wrote XML to file: ${file.displayPath}`);
  } catch (ex) {
    ctx.log.warn(`Failed to create/write XML file: ${messageOf(ex)}`);
    ctx.notifier.error(title, `Failed to create/write XML file: ${messageOf(ex)}`);
  }
}

// ----- GetFileMakerCalculationFromClipboardAction -----

export async function getFileMakerCalculationFromClipboard(ctx: ActionContext, clipboardText?: string): Promise<void> {
  const title = 'Get FileMaker Calculation From Clipboard';
  ctx.log.info('Invoke: GetFileMakerCalculationFromClipboardAction');

  const text = clipboardText ?? (await readClipboard(ctx, title));
  if (text === undefined) return;
  if (javaIsBlank(text)) {
    ctx.notifier.info(title, 'Clipboard is empty or does not contain text to save.');
    return;
  }

  try {
    const file = await ctx.files.createAndOpen(ctx.settings(), '.fmcalc', text);
    ctx.notifier.info(title, `Created: ${file.displayPath}`);
  } catch (ex) {
    ctx.log.warn(`Failed to create/write .fmcalc file: ${messageOf(ex)}`);
    ctx.notifier.error(title, `Failed to create/write .fmcalc file: ${messageOf(ex)}`);
  }
}

// ----- GetFileMakerClipboardContentAction (the "smart" action) -----

export async function getFileMakerClipboardContent(ctx: ActionContext): Promise<void> {
  const title = 'Get FileMaker Clipboard Content';
  ctx.log.info('Invoke: GetFileMakerClipboardContentAction');

  const text = await readClipboard(ctx, title);
  if (text === undefined) return;
  if (javaIsBlank(text)) {
    ctx.notifier.info(title, 'Clipboard is empty or has no text content.');
    return;
  }

  let xml: string;
  try {
    xml = converter.convertToXml(text);
  } catch (ex) {
    if (!(ex instanceof ConversionException)) {
      ctx.notifier.error(title, `Unrecognized clipboard content: ${messageOf(ex)}`);
      return;
    }
    // Not an fmxmlsnippet: treat it as a calculation
    try {
      await getFileMakerCalculationFromClipboard(ctx, text);
    } catch (t) {
      ctx.log.warn(`Delegated calculation action failed: ${messageOf(t)}`);
      ctx.notifier.error(title, `Failed to create .fmcalc file: ${messageOf(t)}`);
    }
    return;
  }

  // 1) Save the XML to a new file (reusing the text already read, instead of reading the clipboard twice)
  try {
    await readClipboardIntoNewXmlFile(ctx, text);
  } catch (t) {
    ctx.log.warn(`Delegated file creation action failed (continuing to clipboard write): ${messageOf(t)}`);
  }

  // 2) Optionally preview, then replace the clipboard with the XML
  if (!(await confirmIfPreviewing(ctx, 'Preview: Replace Clipboard With XML', xml))) {
    ctx.notifier.info(title, 'Canceled: Clipboard was not modified.');
    return;
  }
  try {
    await ctx.clipboard.writeText(xml);
  } catch (ex) {
    ctx.notifier.error(title, `Saved XML to file, but failed to write XML to clipboard: ${messageOf(ex)}`, ex);
    return;
  }
  ctx.notifier.info(title, 'Success: Saved XML to file and replaced clipboard with XML.');
}

// ----- ConvertClipboardToXmlAction (hidden, as in JetBrains) -----

export async function convertClipboardToXml(ctx: ActionContext): Promise<void> {
  const title = 'Convert FM Clipboard To XML Clipboard';
  ctx.log.info('Invoke: ConvertClipboardToXmlAction');

  const text = await readClipboard(ctx, title);
  if (text === undefined) return;
  if (javaIsBlank(text)) {
    ctx.notifier.info(title, 'Clipboard is empty or contains no text to convert.');
    return;
  }

  let xml: string;
  try {
    xml = converter.convertToXml(text);
  } catch (ex) {
    if (ex instanceof ConversionException) {
      ctx.notifier.warn(title, 'Clipboard does not contain recognizable FileMaker content or fmxmlsnippet.');
    } else {
      ctx.notifier.error(title, `Unexpected error during conversion: ${messageOf(ex)}`, ex);
    }
    return;
  }

  if (!(await confirmIfPreviewing(ctx, 'Preview: Convert FM Clipboard To XML Clipboard', xml))) {
    ctx.notifier.info(title, 'Canceled: No changes were made to the clipboard.');
    return;
  }
  try {
    await ctx.clipboard.writeText(xml);
  } catch (ex) {
    ctx.notifier.error(title, `Converted XML generated, but failed to write to clipboard: ${messageOf(ex)}`, ex);
    return;
  }
  ctx.notifier.info(title, 'Success: Converted FileMaker clipboard content to XML and placed it on the clipboard.');
}

// ----- PushClipboardIntoFileMakerAction -----

export function isXmlFileExtension(ext: string | undefined): boolean {
  return ext != null && ext.toLowerCase() === 'xml';
}

export async function pushClipboardIntoFileMaker(ctx: ActionContext, target: PushTarget): Promise<void> {
  const title = 'Push Clipboard Into FileMaker';
  ctx.log.info('Invoke: PushClipboardIntoFileMakerAction');

  if (!isXmlFileExtension(target.extension)) {
    ctx.notifier.warn(title, 'Please focus an XML file to push into FileMaker.');
    return;
  }

  let xml: string;
  try {
    xml = await target.readText();
  } catch (ex) {
    ctx.notifier.error(title, `Failed to read the active XML file: ${messageOf(ex)}`, ex);
    return;
  }
  if (javaIsBlank(xml)) {
    ctx.notifier.info(title, 'The active XML file is empty.');
    return;
  }

  let payload: string;
  try {
    payload = xmlToClipboard.convertToClipboardPayload(xml);
  } catch (ex) {
    if (ex instanceof ConversionException) {
      ctx.log.info(`XML content is not a supported fmxmlsnippet: ${ex.message}`);
      ctx.notifier.warn(title, 'The file does not contain a supported fmxmlsnippet.');
    } else {
      ctx.notifier.error(title, `Unexpected error during conversion: ${messageOf(ex)}`, ex);
    }
    return;
  }

  if (!(await confirmIfPreviewing(ctx, 'Preview: Push Clipboard Into FileMaker', payload))) {
    ctx.notifier.info(title, 'Canceled: No changes were made to the clipboard.');
    return;
  }
  try {
    await ctx.clipboard.writeText(payload);
  } catch (ex) {
    const msg = ex instanceof ClipboardAccessException ? ex.message : messageOf(ex);
    ctx.notifier.error(title, `Converted payload ready, but failed to write to clipboard: ${msg}`, ex);
    return;
  }
  ctx.log.info(`Push successful; payload written to clipboard (bytes=${Buffer.byteLength(payload, 'utf8')})`);
  ctx.notifier.info(title, 'Success: Converted XML and placed FileMaker-compatible content on the clipboard.');
}
