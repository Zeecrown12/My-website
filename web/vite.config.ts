import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig, Plugin } from 'vite';
import react from '@vitejs/plugin-react';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DRAWABLES_DIR = path.resolve(__dirname, '../app/src/main/res/drawable');
const EDITORIAL_IMAGES = [
  'img_cat_adire_abaya.jpg',
  'img_cat_bags_footwear.jpg',
  'img_cat_jewellery_beauty.jpg',
  'img_hero_editorial.jpg',
  'img_zeecrown_edit_campaign.jpg',
];

function editorialDrawablesPlugin(): Plugin {
  return {
    name: 'zeecrown-editorial-drawables',
    configureServer(server) {
      server.middlewares.use('/assets', (req, res, next) => {
        const cleanName = (req.url || '').split('?')[0].replace(/^\/+/, '');
        if (!EDITORIAL_IMAGES.includes(cleanName)) {
          next();
          return;
        }
        const srcPath = path.join(DRAWABLES_DIR, cleanName);
        if (!fs.existsSync(srcPath)) {
          next();
          return;
        }
        res.setHeader('Content-Type', 'image/jpeg');
        fs.createReadStream(srcPath).pipe(res);
      });
    },
    generateBundle() {
      for (const fileName of EDITORIAL_IMAGES) {
        const srcPath = path.join(DRAWABLES_DIR, fileName);
        if (fs.existsSync(srcPath)) {
          this.emitFile({
            type: 'asset',
            fileName: `assets/${fileName}`,
            source: fs.readFileSync(srcPath),
          });
        }
      }
    },
  };
}

export default defineConfig({
  plugins: [react(), editorialDrawablesPlugin()],
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          firebase: ['firebase/app', 'firebase/auth', 'firebase/firestore', 'firebase/storage'],
        },
      },
    },
  },
});
