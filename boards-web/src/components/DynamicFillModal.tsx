import React, { useState, useMemo } from 'react';
import { QuickReply } from '../types/database';
import { parseWhatsAppMarkdownToHtml } from '../lib/whatsappMarkdown';
import { X, Copy, Check, Sparkles, Calculator } from 'lucide-react';

interface DynamicFillModalProps {
  reply: QuickReply;
  onClose: () => void;
  onCopyComplete: (finalText: string) => void;
}

interface DynamicVar {
  name: string;
  defaultValue: string;
  isCalculated: boolean;
  formula: string;
}

export function parseDynamicVariables(template: string): DynamicVar[] {
  const vars: DynamicVar[] = [];
  const regex = /\{([^}]+)\}/g;
  let match: RegExpExecArray | null;

  while ((match = regex.exec(template)) !== null) {
    const raw = match[1].trim();
    if (raw.startsWith('$') || raw.includes('=')) {
      const clean = raw.startsWith('$') ? raw.substring(1).trim() : raw;
      const parts = clean.split('=');
      const varName = parts[0].trim();
      const formula = parts.length > 1 ? parts[1].trim() : '';
      if (!vars.some(v => v.name === varName)) {
        vars.push({ name: varName, defaultValue: '', isCalculated: true, formula });
      }
    } else {
      const parts = raw.split(':');
      const varName = parts[0].trim();
      const defaultVal = parts.length > 1 ? parts[1].trim() : '';
      if (!vars.some(v => v.name === varName)) {
        vars.push({ name: varName, defaultValue: defaultVal, isCalculated: false, formula: '' });
      }
    }
  }

  return vars;
}

export function parseSmartNumber(raw: string): string {
  if (!raw) return '';
  let text = raw.trim().replace(/\$|€|CLP|UF|\s/g, '');
  if (text.includes('.') && text.includes(',')) {
    text = text.replace(/\./g, '').replace(/,/g, '.');
  } else if (text.includes('.') && !text.includes(',')) {
    const parts = text.split('.');
    if (parts.length > 2 || (parts.length === 2 && parts[1].length === 3)) {
      text = text.replace(/\./g, '');
    }
  } else if (text.includes(',') && !text.includes('.')) {
    const parts = text.split(',');
    if (parts.length === 2 && parts[1].length !== 3) {
      text = text.replace(/,/g, '.');
    } else if (parts.length > 2 || (parts.length === 2 && parts[1].length === 3)) {
      text = text.replace(/,/g, '');
    }
  }
  return text.replace(/[^\d.-]/g, '');
}

export function evalMath(exprStr: string): number {
  try {
    let clean = exprStr.replace(/\s+/g, '');
    if (!clean) return 0;

    // Resolver paréntesis
    while (clean.includes('(')) {
      const openIdx = clean.lastIndexOf('(');
      const closeIdx = clean.indexOf(')', openIdx);
      if (closeIdx === -1) break;
      const inside = clean.substring(openIdx + 1, closeIdx);
      const val = evalMath(inside);
      clean = clean.substring(0, openIdx) + val.toString() + clean.substring(closeIdx + 1);
    }

    if (!isNaN(Number(clean))) return Number(clean);

    // Suma y Resta
    for (let i = clean.length - 1; i >= 1; i--) {
      const c = clean[i];
      const prev = clean[i - 1];
      if (c === '+' && !['*', '/', '+', '-'].includes(prev)) {
        return evalMath(clean.substring(0, i)) + evalMath(clean.substring(i + 1));
      } else if (c === '-' && !['*', '/', '+', '-'].includes(prev)) {
        return evalMath(clean.substring(0, i)) - evalMath(clean.substring(i + 1));
      }
    }

    // Multiplicación y División
    for (let i = clean.length - 1; i >= 1; i--) {
      const c = clean[i];
      if (c === '*') {
        return evalMath(clean.substring(0, i)) * evalMath(clean.substring(i + 1));
      } else if (c === '/') {
        const denom = evalMath(clean.substring(i + 1));
        return denom !== 0 ? evalMath(clean.substring(0, i)) / denom : 0;
      }
    }

    return Number(clean) || 0;
  } catch (_) {
    return 0;
  }
}

export function evaluateFormula(formula: string, fieldValues: Record<string, string>): string {
  try {
    let expr = formula;
    Object.entries(fieldValues).forEach(([key, val]) => {
      if (key && val) {
        const cleanNum = parseSmartNumber(val);
        if (cleanNum) {
          expr = expr.replace(new RegExp(`\\b${key}\\b`, 'g'), cleanNum);
        }
      }
    });
    const result = evalMath(expr);
    if (result % 1 === 0) {
      return result.toString();
    }
    return result.toFixed(2);
  } catch (_) {
    return '0';
  }
}

export const DynamicFillModal: React.FC<DynamicFillModalProps> = ({
  reply,
  onClose,
  onCopyComplete,
}) => {
  const parsedVariables = useMemo(() => parseDynamicVariables(reply.content), [reply.content]);

  const [fieldValues, setFieldValues] = useState<Record<string, string>>(() => {
    const initial: Record<string, string> = {};
    parsedVariables.forEach((v) => {
      if (!v.isCalculated) {
        initial[v.name] = v.defaultValue;
      }
    });
    // Calcular iniciales
    parsedVariables.filter((v) => v.isCalculated).forEach((v) => {
      initial[v.name] = evaluateFormula(v.formula, initial);
    });
    return initial;
  });

  const [copied, setCopied] = useState(false);

  const handleFieldChange = (name: string, value: string) => {
    const updated = { ...fieldValues, [name]: value };
    parsedVariables.filter((v) => v.isCalculated).forEach((v) => {
      updated[v.name] = evaluateFormula(v.formula, updated);
    });
    setFieldValues(updated);
  };

  const processedText = useMemo(() => {
    let result = reply.content;
    parsedVariables.forEach((v) => {
      const val = fieldValues[v.name] || (v.isCalculated ? '0' : '');
      const pattern = new RegExp(`\\{${v.name}(?::[^}]+)?\\}|\\{\\$?${v.name}\\s*=\\s*[^}]+\\}`, 'g');
      result = result.replace(pattern, val);
    });
    return result;
  }, [reply.content, parsedVariables, fieldValues]);

  const handleCopy = () => {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(processedText);
    }
    setCopied(true);
    setTimeout(() => {
      onCopyComplete(processedText);
    }, 600);
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '560px' }}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div style={{ background: '#EFF6FF', color: '#2563EB', padding: '6px', borderRadius: '8px' }}>
              <Sparkles size={18} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Personalizar Mensaje</h2>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{reply.title}</p>
            </div>
          </div>
          <button className="icon-button" onClick={onClose}><X size={18} /></button>
        </div>

        <div style={{ padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {/* Campos a rellenar */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)' }}>
              Campos Dinámicos:
            </span>

            {parsedVariables.map((v) => {
              if (v.isCalculated) {
                return (
                  <div
                    key={v.name}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '8px 12px',
                      backgroundColor: '#EFF6FF',
                      border: '1px solid #BFDBFE',
                      borderRadius: '8px',
                    }}
                  >
                    <Calculator size={16} color="#2563EB" />
                    <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#1E40AF' }}>
                      {v.name} = {fieldValues[v.name] || '0'}
                    </span>
                    <span style={{ fontSize: '0.75rem', color: '#60A5FA', marginLeft: 'auto' }}>
                      (Fórmula automática)
                    </span>
                  </div>
                );
              }

              return (
                <div key={v.name} style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    [{v.name}]
                  </label>
                  <input
                    type="text"
                    value={fieldValues[v.name] || ''}
                    onChange={(e) => handleFieldChange(v.name, e.target.value)}
                    placeholder={`Ingresa ${v.name}...`}
                    style={{
                      padding: '8px 12px',
                      borderRadius: '8px',
                      border: '1px solid var(--border)',
                      fontSize: '0.9rem',
                    }}
                    autoFocus={parsedVariables.indexOf(v) === 0}
                  />
                </div>
              );
            })}
          </div>

          {/* Vista previa en tiempo real */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <span style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)' }}>
              Mensaje final resultante:
            </span>
            <div
              style={{
                backgroundColor: 'var(--bg-subtle)',
                padding: '12px',
                borderRadius: '8px',
                fontSize: '0.85rem',
                lineHeight: 1.5,
                border: '1px solid var(--border)',
                maxHeight: '140px',
                overflowY: 'auto',
              }}
              dangerouslySetInnerHTML={{ __html: parseWhatsAppMarkdownToHtml(processedText) }}
            />
          </div>
        </div>

        <div className="modal-footer-full" style={{ padding: '12px 24px' }}>
          <button type="button" className="btn-secondary" onClick={onClose}>
            Cancelar
          </button>
          <button type="button" className="btn-primary" onClick={handleCopy}>
            {copied ? <Check size={16} /> : <Copy size={16} />}
            <span>{copied ? '¡Copiado al Portapapeles!' : 'Copiar Mensaje Personalizado'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
