// Port of jetbrains/.../fm/ConversionException.java. Messages are user-facing and pinned by shared/fixtures.
export class ConversionException extends Error {
  constructor(message: string, options?: { cause?: unknown }) {
    super(message, options);
    this.name = 'ConversionException';
  }
}
