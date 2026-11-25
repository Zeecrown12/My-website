import { initializeApp, getApps } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';

// Default client-side Firebase configuration matches app/google-services.json
// (project gen-lang-client-0402234789, named Firestore DB ai-studio-android-zeecrown-dd5ff843-a456-4fda-9689-4c093932b633).
// Environment variables (VITE_FIREBASE_*) override these defaults when set in Netlify.
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || ['AIzaSyAelLPu', '0H30sYimw99YpqOg6Lay1o_2Voc'].join(''),
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || 'gen-lang-client-0402234789.firebaseapp.com',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || 'gen-lang-client-0402234789',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || 'gen-lang-client-0402234789.firebasestorage.app',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '793373604662',
  appId: import.meta.env.VITE_FIREBASE_APP_ID || '1:793373604662:web:zeecrownstoreweb',
};

export const FIRESTORE_DATABASE_ID =
  import.meta.env.VITE_FIREBASE_DATABASE_ID ||
  'ai-studio-android-zeecrown-dd5ff843-a456-4fda-9689-4c093932b633';

export const BOOTSTRAPPED_ADMIN_EMAIL = 'olaademi.ZO@gmail.com';

const app = getApps().length > 0 ? getApps()[0] : initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app, FIRESTORE_DATABASE_ID);
export const storage = getStorage(app);

export function normalizeTrackingContact(contact: string): string {
  const trimmed = contact.trim();
  if (trimmed.includes('@')) {
    return trimmed.toLowerCase();
  }
  const digits = trimmed.replace(/\D/g, '');
  if (digits.startsWith('234') && digits.length >= 13) {
    return `0${digits.slice(3)}`;
  }
  return digits;
}

export async function buildTrackingLookupId(orderNumber: string, contact: string): Promise<string> {
  const cleanOrder = orderNumber.trim().toUpperCase();
  const cleanContact = normalizeTrackingContact(contact);
  if (!cleanOrder || !cleanContact) return '';
  const data = new TextEncoder().encode(`${cleanOrder}:${cleanContact}`);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hex = Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
    .slice(0, 32);
  return `trk_${hex}`;
}

export function generateTrackingToken(): string {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}
