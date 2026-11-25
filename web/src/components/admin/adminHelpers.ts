import { addDoc, collection, serverTimestamp } from 'firebase/firestore';
import { getDownloadURL, ref, uploadBytes } from 'firebase/storage';
import { db, storage } from '../../lib/firebase';
import { AdminRole } from '../../types/store';

export interface AdminActor {
  uid: string;
  email: string;
  role: AdminRole | 'none';
}

export async function recordAdminAudit(
  actor: AdminActor,
  action: string,
  targetType: string,
  targetId: string,
  summary: string,
  previousValue = '',
  newValue = ''
): Promise<void> {
  if (!actor.uid || actor.role === 'none') return;
  try {
    await addDoc(collection(db, 'adminActivityLogs'), {
      adminUid: actor.uid,
      adminEmail: (actor.email || actor.uid).slice(0, 200),
      adminRole: String(actor.role).slice(0, 40),
      action: action.slice(0, 80),
      targetType: targetType.slice(0, 60),
      targetId: (targetId || 'global').slice(0, 128),
      summary: summary.slice(0, 320),
      ...(previousValue ? { previousValue: previousValue.slice(0, 300) } : {}),
      ...(newValue ? { newValue: newValue.slice(0, 300) } : {}),
      createdAt: serverTimestamp(),
    });
  } catch {
    // Audit logging should never block primary admin operations
  }
}

export async function uploadProductPhotoFile(
  file: File,
  targetProductId: string
): Promise<{ url: string; provider: 'cloudinary' | 'firebase' }> {
  const cloudName = (import.meta.env.VITE_CLOUDINARY_CLOUD_NAME || '').trim();
  const uploadPreset = (import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET || '').trim();

  if (cloudName && uploadPreset) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', uploadPreset);
    formData.append('folder', `zeecrown/products/${targetProductId}`);

    const response = await fetch(`https://api.cloudinary.com/v1_1/${encodeURIComponent(cloudName)}/image/upload`, {
      method: 'POST',
      body: formData,
    });
    if (!response.ok) {
      throw new Error(`Cloudinary upload returned HTTP ${response.status}`);
    }
    const payload = (await response.json()) as { secure_url?: string };
    if (!payload.secure_url) {
      throw new Error('Cloudinary response did not include secure_url.');
    }
    return { url: payload.secure_url, provider: 'cloudinary' };
  }

  const cleanName = `${Date.now()}_${file.name.replace(/[^a-zA-Z0-9._-]/g, '_')}`;
  const storageRef = ref(storage, `products/${targetProductId}/${cleanName}`);
  await uploadBytes(storageRef, file, { contentType: file.type || 'image/jpeg' });
  const url = await getDownloadURL(storageRef);
  return { url, provider: 'firebase' };
}
