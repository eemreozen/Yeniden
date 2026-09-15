export function toE164(raw: string): string {
  const digits = raw.replace(/\D/g, '');
  if (digits.startsWith('90') && digits.length === 12) return `+${digits}`;
  if (digits.length === 10 && digits.startsWith('5')) return `+90${digits}`;
  throw new Error('Geçerli bir Türkiye cep telefonu girin.');
}
