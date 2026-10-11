import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        proxy: {
            '/api/chat': 'http://localhost:8092',
            '/api': 'http://localhost:8090',
            '/ws': { target: 'ws://localhost:8092', ws: true },
            '/images': 'http://localhost:8090',
            '/oauth2': 'http://localhost:8090',
            '/login/oauth2': 'http://localhost:8090',
        },
    },
});
