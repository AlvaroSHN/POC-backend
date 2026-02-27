import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { FiActivity, FiAlertTriangle, FiCheckCircle, FiChevronDown, FiChevronUp, FiDatabase, FiRefreshCw } from 'react-icons/fi';
import BPMNViewer from './BPMNViewer';
import './App.css';

const failOptions = ['NONE', 'PROTOCOL', 'INTERACTION', 'INTERACTION_ITEM', 'CASE', 'NOTIFICATION'];

const defaultForm = () => ({
  externalId: `CASE-${Math.floor(Math.random() * 10000)}`,
  description: 'Cliente abriu reclamação de indisponibilidade',
  clientType: 'GOLD',
  origin: 'APP',
  simulateFailAt: 'NONE'
});

const parseMapText = (text) => {
  const trimmed = (text || '').trim();
  if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) return trimmed;

  const inner = trimmed.slice(1, -1).trim();
  if (!inner) return {};

  const result = {};
  let token = '';
  let level = 0;
  const chunks = [];

  for (let i = 0; i < inner.length; i += 1) {
    const char = inner[i];
    if (char === '{') level += 1;
    if (char === '}') level -= 1;

    if (char === ',' && level === 0) {
      chunks.push(token.trim());
      token = '';
      continue;
    }
    token += char;
  }
  if (token.trim()) chunks.push(token.trim());

  chunks.forEach((chunk) => {
    const idx = chunk.indexOf('=');
    if (idx === -1) {
      result[chunk] = null;
      return;
    }
    const key = chunk.slice(0, idx).trim();
    const value = chunk.slice(idx + 1).trim();
    result[key] = value.startsWith('{') && value.endsWith('}') ? parseMapText(value) : value;
  });

  return result;
};

const parseTransformationDetails = (details) => {
  if (!details || !details.includes('before=') || !details.includes('| after=')) return null;

  const [beforePart, afterPart] = details.split('| after=');
  const beforeRaw = beforePart.replace(/^before=/, '').trim();
  const afterRaw = (afterPart || '').trim();

  return {
    before: parseMapText(beforeRaw),
    after: parseMapText(afterRaw)
  };
};

function App() {
  const [formData, setFormData] = useState(defaultForm());
  const [tickets, setTickets] = useState([]);
  const [selectedExternalId, setSelectedExternalId] = useState('');
  const [contexts, setContexts] = useState([]);
  const [activeContextId, setActiveContextId] = useState('');
  const [currentStatus, setCurrentStatus] = useState('UNKNOWN');
  const [contextSelectorOpen, setContextSelectorOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const selectedTimelineId = useMemo(() => activeContextId || selectedExternalId || formData.externalId, [activeContextId, selectedExternalId, formData.externalId]);
  const activeContext = useMemo(() => contexts.find((ctx) => ctx.externalId === selectedTimelineId), [contexts, selectedTimelineId]);
  const history = activeContext?.history || [];

  const fetchTickets = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/v1/process/tickets');
      const data = await res.json();
      setTickets(Array.isArray(data) ? data.reverse() : []);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchHistory = useCallback(async (externalId) => {
    if (!externalId) return;
    const [historyRes, statusRes] = await Promise.all([
      fetch(`/api/v1/process/${externalId}/history`),
      fetch(`/api/v1/process/${externalId}/status`)
    ]);

    const historyData = await historyRes.json();
    const statusData = await statusRes.json();
    const safeHistory = Array.isArray(historyData) ? historyData : [];
    const status = statusData.currentStatus || 'UNKNOWN';

    setContexts((prev) => {
      const existing = prev.filter((ctx) => ctx.externalId !== externalId);
      return [...existing, { externalId, history: safeHistory, currentStatus: status, updatedAt: new Date().toISOString() }]
        .sort((a, b) => a.updatedAt.localeCompare(b.updatedAt));
    });

    if (selectedTimelineId === externalId) {
      setCurrentStatus(status);
    }
  }, [selectedTimelineId]);

  useEffect(() => { fetchTickets(); }, [fetchTickets]);

  useEffect(() => {
    if (!selectedTimelineId) return;
    fetchHistory(selectedTimelineId);
    const interval = setInterval(() => fetchHistory(selectedTimelineId), 2500);
    return () => clearInterval(interval);
  }, [selectedTimelineId, fetchHistory]);

  const triggerSimulation = async () => {
    const res = await fetch('/api/v1/process/simulate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData)
    });

    const data = await res.json();
    const externalId = data.externalId;
    setSelectedExternalId(externalId);
    setActiveContextId(externalId);
    setContextSelectorOpen(false);
    setTimeout(fetchTickets, 1800);
  };

  const selectContext = (externalId) => {
    setActiveContextId(externalId);
    setSelectedExternalId(externalId);
    setContextSelectorOpen(false);
  };

  return (
    <div className="app-container">
      <header className="app-header"><h1>⚡ Thunderstruck Full Capability Test Panel</h1></header>
      <main className="content-single-page">
        <div className="simulator-container">
          <div className="section-header"><FiActivity /><h2>Simulação End2End (SAGA + DMN + UserTask)</h2></div>
          <BPMNViewer diagramUrl="/thunderstruck-camunda7-saga.bpmn" />

          <div className="form-grid">
            <div className="field"><label>External ID</label><input value={formData.externalId} onChange={(e) => setFormData({ ...formData, externalId: e.target.value })} /></div>
            <div className="field"><label>Descrição da reclamação</label><input value={formData.description} onChange={(e) => setFormData({ ...formData, description: e.target.value })} /></div>
            <div className="field"><label>Tipo cliente</label><select value={formData.clientType} onChange={(e) => setFormData({ ...formData, clientType: e.target.value })}><option value="GOLD">GOLD</option><option value="STANDARD">STANDARD</option></select></div>
            <div className="field"><label>Origem</label><select value={formData.origin} onChange={(e) => setFormData({ ...formData, origin: e.target.value })}><option value="APP">APP</option><option value="WEB">WEB</option><option value="ADMIN">ADMIN</option></select></div>
            <div className="field" style={{ gridColumn: 'span 2' }}><label>Falha simulada</label><select value={formData.simulateFailAt} onChange={(e) => setFormData({ ...formData, simulateFailAt: e.target.value })}>{failOptions.map(x => <option key={x} value={x}>{x}</option>)}</select></div>
          </div>

          <div className="button-row">
            <button className="btn-primary success" onClick={triggerSimulation}><FiCheckCircle /> Executar simulação</button>
            <button className="btn-primary danger" onClick={() => setFormData(defaultForm())}><FiAlertTriangle /> Novo cenário</button>
          </div>

          <div className="status-panel">
            <strong>Status Atual:</strong>
            <span>{currentStatus}</span>
            <button className="btn-refresh" onClick={() => fetchHistory(selectedTimelineId)}><FiRefreshCw /></button>
          </div>

          <div className="context-selector">
            <button className="context-toggle" onClick={() => setContextSelectorOpen((v) => !v)}>
              <span>Contextos de simulação ({contexts.length})</span>
              {contextSelectorOpen ? <FiChevronUp /> : <FiChevronDown />}
            </button>
            {contextSelectorOpen && (
              <div className="context-list">
                {contexts.length === 0 && <div className="context-item muted">Nenhuma simulação registrada ainda.</div>}
                {[...contexts].reverse().map((ctx) => (
                  <button
                    key={ctx.externalId}
                    className={`context-item ${ctx.externalId === selectedTimelineId ? 'active' : ''}`}
                    onClick={() => selectContext(ctx.externalId)}
                  >
                    <div>{ctx.externalId}</div>
                    <small>{ctx.currentStatus}</small>
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="timeline">
            <h3>Transformação por caixinha BPMN ({selectedTimelineId || 'execute um cenário'})</h3>
            {history.map((item) => {
              const parsed = parseTransformationDetails(item.details);
              return (
                <div key={item.id} className="timeline-item">
                  <div><strong>{item.stage}</strong> • {item.source}</div>
                  <div>{item.previousStatus || '-'} → <strong>{item.currentStatus}</strong></div>
                  {parsed ? (
                    <div className="diff-grid">
                      <div>
                        <strong>Before</strong>
                        <pre>{JSON.stringify(parsed.before, null, 2)}</pre>
                      </div>
                      <div>
                        <strong>After</strong>
                        <pre>{JSON.stringify(parsed.after, null, 2)}</pre>
                      </div>
                    </div>
                  ) : (
                    <pre className="raw-details">{item.details}</pre>
                  )}
                  <small>{item.createdAt}</small>
                </div>
              );
            })}
          </div>

          <div className="tmf-section">
            <div className="section-header" style={{ color: '#a6e3a1' }}><FiDatabase /><h2>TroubleTicket (TMF621)</h2><button className="btn-refresh" onClick={fetchTickets} disabled={loading}><FiRefreshCw className={loading ? 'spin' : ''} /></button></div>
            <div className="ticket-table-wrapper">
              <table className="ticket-table">
                <thead><tr><th>ID</th><th>External ID</th><th>Status</th><th>Severity</th><th>Origem</th></tr></thead>
                <tbody>{tickets.map((t) => <tr key={t.id} onClick={() => selectContext(t.externalId)}><td>{t.id}</td><td>{t.externalId}</td><td>{t.status}</td><td>{t.severity}</td><td>{t.relatedPartyId}</td></tr>)}</tbody>
              </table>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;
