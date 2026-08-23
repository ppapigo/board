// @ts-ignore
import { StrictMode } from 'react';
// @ts-ignore
import { createRoot } from 'react-dom/client';
// @ts-ignore
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './auth';
import App from './App';
import './styles.css';

// @ts-ignore
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider><App /></AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
