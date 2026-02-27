import React, { useState, useEffect } from 'react';
import { FiPlay, FiActivity, FiExternalLink, FiDatabase, FiList, FiRefreshCw } from 'react-icons/fi';
import BPMNViewer from './BPMNViewer';
import './App.css';

function App() {
  const [formData, setFormData] = useState({
    externalId: 'VIVO-REQ-' + Math.floor(Math.random() * 1000),
    description: 'Instalação de Fibra Óptica',
    clientType: 'GOLD',
    origin: 'APP'
  });
  const [apiResponse, setApiResponse] = useState(null);
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(false);

  // Função para buscar tickets do banco (TMF621)
  const fetchTickets = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/v1/process/tickets');
      const data = await res.json();
      setTickets(Array.isArray(data) ? data.reverse() : []);
    } catch (e) { console.error("Erro ao buscar tickets", e); }
    setLoading(false);
  };

  useEffect(() => { fetchTickets(); }, []);

  const dispararPostBFF = async () => {
    setApiResponse({ type: 'info', message: 'Enviando para o Motor...' });
    const payload = {
      externalId: formData.externalId,
      description: formData.description,
      status: "INITIAL",
      clientType: formData.clientType, // Adicionado
      origin: formData.origin          // Adicionado
    };
    try {
      const res = await fetch('/api/v1/process', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await res.json();
      setApiResponse({ type: 'success', data });
      // Refresh automático após 3 segundos (tempo do Camunda processar)
      setTimeout(fetchTickets, 3000);
    } catch (error) {
      setApiResponse({ type: 'error', message: 'Erro de conexão com o BFF.' });
    }
  };

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-content">
          <h1>⚡ ThunderStruck POC - TMF Orchestrator</h1>
          <div className="external-links">
            <a href="http://localhost:8081" target="_blank" rel="noreferrer"><FiExternalLink /> Operate</a>
            <a href="http://localhost:8082" target="_blank" rel="noreferrer"><FiExternalLink /> Tasklist</a>
          </div>
        </div>
      </header>
      
      <main className="content-single-page">
        <div className="simulator-container">
          <div className="section-header">
            <FiActivity /> <h2>Simulador de Orquestração</h2>
          </div>
          
          <BPMNViewer diagramUrl="/thunderstruck-process-usertasks-v3.bpmn" />
          
          <div className="form-grid">
            <div className="field"><label>ID Externo</label><input name="externalId" value={formData.externalId} onChange={(e) => setFormData({...formData, externalId: e.target.value})} /></div>
            <div className="field"><label>Descrição</label><input name="description" value={formData.description} onChange={(e) => setFormData({...formData, description: e.target.value})} /></div>
            <div className="field"><label>DMN: Perfil</label><select name="clientType" value={formData.clientType} onChange={(e) => setFormData({...formData, clientType: e.target.value})}><option value="GOLD">GOLD</option><option value="STANDARD">STANDARD</option></select></div>
            <div className="field"><label>DMN: Origem</label><select name="origin" value={formData.origin} onChange={(e) => setFormData({...formData, origin: e.target.value})}><option value="APP">APP</option><option value="WEB">WEB</option></select></div>
          </div>
          
          <button className="btn-primary" onClick={dispararPostBFF}><FiPlay /> Disparar Evento via API (BFF)</button>

          <div className="tmf-section">
            <div className="section-header" style={{color: '#a6e3a1', marginTop: '40px'}}>
              <FiList /> <h2>Monitor Canônico TMF621 (Banco Oracle)</h2>
              <button className="btn-refresh" onClick={fetchTickets} disabled={loading}><FiRefreshCw className={loading ? 'spin' : ''} /></button>
            </div>
            <div className="ticket-table-wrapper">
              <table className="ticket-table">
                <thead>
                  <tr><th>ID TMF</th><th>External ID</th><th>Severity</th><th>Status (TMF)</th><th>Related Party</th></tr>
                </thead>
                <tbody>
                  {tickets.map(t => (
                    <tr key={t.id}><td>{t.id}</td><td>{t.externalId}</td><td className={t.severity}>{t.severity}</td><td>{t.status}</td><td>{t.relatedPartyId}</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="oracle-instruction">
            <div className="instruction-header"><FiDatabase /> <strong>Gatilho via Banco (CDC)</strong></div>
            <code>
              INSERT INTO C##DBZUSER.PROCESS_REQUEST (EXTERNAL_ID, STATUS, DESCRIPTION, CLIENT_TYPE, ORIGIN) <br/>
              VALUES ('{formData.externalId}', 'NOVO', '{formData.description}', '{formData.clientType}', '{formData.origin}'); COMMIT;
            </code>
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;