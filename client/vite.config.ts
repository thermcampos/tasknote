import { ConfigEnv, defineConfig, UserConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'url';
import path from 'path';

const proxyConfig = process.env.NGROK
  ? {
      '/api': {
        target: `http://${process.env.BACKEND_HOST || 'localhost'}:8585`,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    }
  : undefined;

export default defineConfig(({ mode }: ConfigEnv) => {
  const config: UserConfig = {
    define: {},
    plugins: [
      {
        name: 'build-html',
        apply: 'build',
        transformIndexHtml: (html) => {
          return {
            html,
            tags: [
              {
                tag: 'script',
                attrs: {
                  src: '/env.js'
                },
                injectTo: 'head'
              }
            ]
          };
        }
      },
      react()
    ],
    build: {
      outDir: 'dist',
      sourcemap: mode === 'development'
    },
    server: {
      port: 5000,
      ...(process.env.NGROK ? { allowedHosts: ['.ngrok-free.dev'] } : {}),
      proxy: proxyConfig,
    },
    preview: {
      port: 5000
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
        '~bootstrap': path.resolve(__dirname, 'node_modules/bootstrap')
      }
    }
  };

  if (mode === 'development') {
    if (config.define) {
      config.define.global = {};
    }
  }

  return config;
});
