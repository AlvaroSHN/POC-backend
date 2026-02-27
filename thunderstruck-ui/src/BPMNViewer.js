import React, { useEffect, useRef } from 'react';
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer'; // Usamos o NavigatedViewer para permitir zoom e pan

// Importando os estilos obrigatórios do bpmn-js
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';

const BPMNViewer = ({ diagramUrl }) => {
  const containerRef = useRef(null);

  useEffect(() => {
    // Inicializa o visualizador
    const viewer = new BpmnViewer({
      container: containerRef.current,
    });

    const loadDiagram = async () => {
      try {
        // Busca o XML do arquivo na pasta public
        const response = await fetch(diagramUrl);
        const xml = await response.text();
        
        // Importa para o canvas do Camunda
        await viewer.importXML(xml);
        
        // Ajusta o zoom para caber certinho no container
        const canvas = viewer.get('canvas');
        canvas.zoom('fit-viewport');
      } catch (err) {
        console.error('Erro ao carregar o diagrama BPMN:', err);
      }
    };

    loadDiagram();

    // Limpa a instância quando o componente for desmontado
    return () => {
      viewer.destroy();
    };
  }, [diagramUrl]);

  return (
    <div 
      ref={containerRef} 
      style={{ 
        width: '100%', 
        height: '350px', 
        backgroundColor: '#ffffff', // Fundo branco para contrastar com o dark mode
        borderRadius: '8px', 
        marginBottom: '20px',
        border: '1px solid #45475a',
        overflow: 'hidden'
      }} 
    />
  );
};

export default BPMNViewer;