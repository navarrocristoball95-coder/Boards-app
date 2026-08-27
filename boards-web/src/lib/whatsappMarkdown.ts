export function formatSelection(
  text: string,
  selectionStart: number,
  selectionEnd: number,
  delimiter: string
): { newText: string; newStart: number; newEnd: number } {
  const selectedText = text.substring(selectionStart, selectionEnd);
  
  if (selectedText.startsWith(delimiter) && selectedText.endsWith(delimiter) && selectedText.length >= delimiter.length * 2) {
    const unformatted = selectedText.slice(delimiter.length, -delimiter.length);
    const newText = text.substring(0, selectionStart) + unformatted + text.substring(selectionEnd);
    return {
      newText,
      newStart: selectionStart,
      newEnd: selectionStart + unformatted.length,
    };
  }

  const formatted = `${delimiter}${selectedText}${delimiter}`;
  const newText = text.substring(0, selectionStart) + formatted + text.substring(selectionEnd);
  return {
    newText,
    newStart: selectionStart,
    newEnd: selectionStart + formatted.length,
  };
}

export function applyListFormat(
  text: string,
  selectionStart: number,
  selectionEnd: number,
  type: 'bullet' | 'numbered' | 'quote'
): { newText: string; newStart: number; newEnd: number } {
  const lineStart = selectionStart <= 0 ? 0 : (() => {
    const prevNl = text.lastIndexOf('\n', selectionStart - 1);
    return prevNl === -1 ? 0 : prevNl + 1;
  })();
  
  const nextNl = text.indexOf('\n', selectionEnd);
  const lineEnd = nextNl === -1 ? text.length : nextNl;

  const targetBlock = text.substring(lineStart, lineEnd);
  const lines = targetBlock.split('\n');

  let numIndex = 1;
  const newLines = lines.map((line) => {
    if (type === 'bullet') {
      if (line.startsWith('• ') || line.startsWith('- ')) {
        return line.substring(2);
      }
      return `• ${line}`;
    } else if (type === 'numbered') {
      const regex = /^\d+\.\s*/;
      if (regex.test(line)) {
        return line.replace(regex, '');
      }
      return `${numIndex++}. ${line}`;
    } else {
      if (line.startsWith('> ')) {
        return line.substring(2);
      }
      return `> ${line}`;
    }
  });

  const formattedBlock = newLines.join('\n');
  const newText = text.substring(0, lineStart) + formattedBlock + text.substring(lineEnd);
  return {
    newText,
    newStart: lineStart,
    newEnd: lineStart + formattedBlock.length,
  };
}

export function parseWhatsAppMarkdownToHtml(rawText: string): string {
  if (!rawText) return '';

  let html = rawText
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  // Triple backtick Monospace ```text```
  html = html.replace(/```([^`]+)```/g, '<code class="wa-mono">$1</code>');

  // Bold *text*
  html = html.replace(/(^|[^\w*])\*([^\s*](?:.*?[^\s*])?)\*(?=[^\w*]|$)/g, '$1<strong>$2</strong>');

  // Italic _text_
  html = html.replace(/(^|[^\w_])_([^\s_](?:.*?[^\s_])?)_(?=[^\w_]|$)/g, '$1<em>$2</em>');

  // Strikethrough ~text~
  html = html.replace(/(^|[^\w~])~([^\s~](?:.*?[^\s~])?)~(?=[^\w~]|$)/g, '$1<del>$2</del>');

  // Dynamic variables highlight {variable}
  html = html.replace(/\{([^}]+)\}/g, '<span class="wa-variable">{$1}</span>');

  // Line breaks
  html = html.replace(/\n/g, '<br/>');

  // WhatsApp Blockquote > text
  html = html.replace(/(?:^|<br\/>)&gt;\s*(.*?)(?=<br\/>|$)/g, '<div class="wa-quote">$1</div>');

  return html;
}

export function formatLinkMessage(accompanyingText?: string | null, shortName?: string | null, url?: string | null): string {
  let extractedUrl = (url || '').trim();
  const rawText = (accompanyingText || '').trim();

  if (!extractedUrl && rawText) {
    const match = rawText.match(/https?:\/\/[^\s)]+/);
    if (match) {
      extractedUrl = match[0];
    }
  }

  if (!extractedUrl && !rawText) return '';

  // Remove markdown link syntax [text](url) or text (url)
  let cleanAccompanying = rawText.replace(/\[?([^\]\n]+)\]?\s*\((https?:\/\/[^\)]+)\)/g, '$1');

  if (extractedUrl) {
    cleanAccompanying = cleanAccompanying.replace(extractedUrl, '');
  }

  if (shortName && shortName.trim()) {
    const escaped = shortName.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    cleanAccompanying = cleanAccompanying.replace(new RegExp(`\\s*${escaped}\\s*$`, 'i'), '');
  }

  cleanAccompanying = cleanAccompanying.trim();

  if (cleanAccompanying && extractedUrl) {
    return `${cleanAccompanying}\n${extractedUrl}`;
  }
  return extractedUrl || cleanAccompanying;
}
