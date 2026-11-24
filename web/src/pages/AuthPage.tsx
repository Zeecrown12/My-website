import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  createUserWithEmailAndPassword,
  GoogleAuthProvider,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  updateProfile,
} from 'firebase/auth';
import { doc, serverTimestamp, setDoc } from 'firebase/firestore';
import { auth, db } from '../lib/firebase';
import { useStore } from '../context/StoreContext';

export function AuthPage({ initialMode = 'login' }: { initialMode?: 'login' | 'register' }) {
  const { currency } = useStore();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = (location.state as { from?: string } | null)?.from || '/account';

  const [mode, setMode] = useState<'login' | 'register' | 'reset'>(initialMode);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const handleGoogle = async () => {
    setBusy(true);
    setError(null);
    try {
      const provider = new GoogleAuthProvider();
      await signInWithPopup(auth, provider);
      navigate(redirectTo);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Google Sign-In failed.');
    } finally {
      setBusy(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      if (mode === 'reset') {
        await sendPasswordResetEmail(auth, email.trim());
        setNotice('Password reset instructions sent to your email address.');
        setMode('login');
      } else if (mode === 'register') {
        const cred = await createUserWithEmailAndPassword(auth, email.trim(), password);
        if (name.trim()) {
          await updateProfile(cred.user, { displayName: name.trim().slice(0, 80) });
        }
        await setDoc(doc(db, 'users', cred.user.uid), {
          userId: cred.user.uid,
          displayName: (name.trim() || email.split('@')[0] || 'Client').slice(0, 80),
          preferredCurrency: currency,
          role: 'customer',
          emailVerified: cred.user.emailVerified,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
        await setDoc(doc(db, 'users', cred.user.uid, 'private', 'info'), {
          userId: cred.user.uid,
          email: email.trim().slice(0, 200),
          updatedAt: serverTimestamp(),
        });
        navigate(redirectTo);
      } else {
        await signInWithEmailAndPassword(auth, email.trim(), password);
        navigate(redirectTo);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication failed.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container section" style={{ maxWidth: 480 }}>
      <div className="card" style={{ padding: 28 }}>
        <h1 style={{ marginBottom: 6 }}>
          {mode === 'login'
            ? 'Welcome Back'
            : mode === 'register'
            ? 'Create Account'
            : 'Reset Password'}
        </h1>
        <p style={{ color: '#5C4B6E', marginTop: 0, marginBottom: 20 }}>
          Sign in to sync your shopping bag, wishlist, saved addresses, and live order tracking across web and Android.
        </p>

        {error && <div className="alert alert-error">{error}</div>}
        {notice && <div className="alert alert-success">{notice}</div>}

        <button
          type="button"
          className="btn btn-outline"
          disabled={busy}
          onClick={() => void handleGoogle()}
          style={{ width: '100%', marginBottom: 18 }}
        >
          Continue with Google
        </button>

        <form onSubmit={(e) => void handleSubmit(e)}>
          {mode === 'register' && (
            <div className="field">
              <label>Full Name</label>
              <input
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Adaeze Okafor"
              />
            </div>
          )}

          <div className="field">
            <label>Email Address</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
            />
          </div>

          {mode !== 'reset' && (
            <div className="field">
              <label>Password</label>
              <input
                type="password"
                required
                minLength={6}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Minimum 6 characters"
              />
            </div>
          )}

          <button type="submit" className="btn" disabled={busy} style={{ width: '100%' }}>
            {busy
              ? 'Please wait...'
              : mode === 'login'
              ? 'Sign In'
              : mode === 'register'
              ? 'Create Account'
              : 'Send Reset Email'}
          </button>
        </form>

        <div
          style={{
            marginTop: 18,
            display: 'flex',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 8,
            fontSize: '0.86rem',
          }}
        >
          {mode !== 'register' ? (
            <button
              type="button"
              onClick={() => setMode('register')}
              style={{ background: 'none', border: 'none', color: '#542080', fontWeight: 600, cursor: 'pointer', padding: 0 }}
            >
              Need an account? Register
            </button>
          ) : (
            <button
              type="button"
              onClick={() => setMode('login')}
              style={{ background: 'none', border: 'none', color: '#542080', fontWeight: 600, cursor: 'pointer', padding: 0 }}
            >
              Already registered? Sign In
            </button>
          )}
          {mode !== 'reset' && (
            <button
              type="button"
              onClick={() => setMode('reset')}
              style={{ background: 'none', border: 'none', color: '#5C4B6E', cursor: 'pointer', padding: 0 }}
            >
              Forgot Password?
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
