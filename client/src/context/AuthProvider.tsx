import React, { useEffect, useMemo, useState } from 'react';
import AuthContext, { AuthContextData } from './AuthContext';
import { API_TOKEN, REDIRECT_PATH, USER_DATA } from '../app-constants/app-constants';
import { SignInResponse } from '../types/SigninResponse';
import api, { setUnauthorizedHandler } from '../api-service/api';
import ApiConfig from '../api-service/apiConfig';
import { UserResponse } from '../types/UserResponse';
import { UserRegistration } from '../types/UserRegistration';
import { clearHomeCache } from '../utils/HomeCache';

interface Props {
  children: React.ReactNode;
}

const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }: Props) => {
  const [signed, setSigned] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);
  const [user, setUser] = useState<UserResponse | undefined>();
  const [isAdmin, setIsAdmin] = useState<boolean>(false);

  const fetchCurrentSession = async (pathname: string): Promise<SignInResponse | undefined> => {
    const token = localStorage.getItem(API_TOKEN);
    if (!token) {
      return undefined;
    }
    try {
      const bearerToken: SignInResponse = await api.getJSON(ApiConfig.refreshTokenUrl);
      return bearerToken;
    }
    catch (e) {
      if (e instanceof Error) {
        if (e.message !== 'No saved token!') {
          console.warn(e.message);
        }
      }
      else if (e) {
        console.warn(e);
      }
      handleSessionExpired(pathname);
    }
    return undefined;
  };

  const updateUserSession = (userPriv: UserResponse | null, bearerToken: string): UserResponse | null => {
    if (userPriv) {
      localStorage.setItem(USER_DATA, JSON.stringify(userPriv));
    }
    localStorage.setItem(API_TOKEN, bearerToken);

    if (userPriv) {
      return userPriv;
    }

    const savedUser = localStorage.getItem(USER_DATA);
    if (savedUser) {
      return JSON.parse(savedUser);
    }

    return null;
  };

  const checkCurrentAuthUser = async (pathname: string): Promise<void> => {
    const bearerToken: SignInResponse | undefined = await fetchCurrentSession(pathname);
    if (bearerToken && bearerToken.token) {
      const currentUser: UserResponse = await api.getJSON(ApiConfig.currentUserUrl);
      const userLocal = updateUserSession(currentUser, bearerToken.token);
      if (userLocal) {
        setSigned(true);
        setUser(userLocal);
      }
    }
  };

  const register = async (payload: UserRegistration): Promise<string> => {
    try {
      await api.putJSON(ApiConfig.registerUrl, payload);
      return Promise.resolve('OK');
    }
    catch (e) {
      if (e instanceof Error) {
        return Promise.reject(e);
      }
      return Promise.reject(new Error('Unknown error!'));
    }
  };

  const signIn = async (email: string, password: string): Promise<string> => {
    try {
      const payload = { email, password };
      const registerResponse: SignInResponse = await api.postJSON(ApiConfig.signInUrl, payload);
      localStorage.setItem(API_TOKEN, registerResponse.token);
      const currentUser: UserResponse = await api.getJSON(ApiConfig.currentUserUrl);

      setSigned(true);
      setUser(currentUser);
      updateUserSession(currentUser, registerResponse.token);
      return Promise.resolve('OK');
    }
    catch (e) {
      return Promise.reject(e);
    }
  };

  const signOut = (): void => {
    setSigned(false);
    setUser(undefined);
    setIsAdmin(false);
    localStorage.removeItem(API_TOKEN);
    localStorage.removeItem(REDIRECT_PATH);
    localStorage.removeItem(USER_DATA);
    clearHomeCache();
  };

  const handleSessionExpired = (pathname: string): void => {
    signOut();
    if (pathname) {
      localStorage.setItem(REDIRECT_PATH, pathname);
    }
  };

  useEffect(() => {
    setUnauthorizedHandler(() => handleSessionExpired(window.location.pathname));
    return () => setUnauthorizedHandler(undefined);
  }, []);

  useEffect(() => {
    checkCurrentAuthUser(window.location.pathname)
      .catch(e => console.error(e))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!signed) return;

    const TWENTY_FIVE_MINUTES = 25 * 60 * 1000;
    const WAKE_REFRESH_THROTTLE = 60 * 1000;
    let lastWakeRefreshAt = 0;

    const refreshSession = () => {
      checkCurrentAuthUser(window.location.pathname).catch(() => {
        handleSessionExpired(window.location.pathname);
      });
    };

    // Timers do not fire while the machine sleeps or the tab is frozen, so
    // revalidate the session as soon as the user comes back to the tab.
    const refreshOnWake = () => {
      const now = Date.now();
      if (now - lastWakeRefreshAt < WAKE_REFRESH_THROTTLE) return;
      lastWakeRefreshAt = now;
      refreshSession();
    };

    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        refreshOnWake();
      }
    };

    const intervalId = setInterval(refreshSession, TWENTY_FIVE_MINUTES);
    document.addEventListener('visibilitychange', onVisibilityChange);
    window.addEventListener('focus', refreshOnWake);
    window.addEventListener('online', refreshOnWake);

    return () => {
      clearInterval(intervalId);
      document.removeEventListener('visibilitychange', onVisibilityChange);
      window.removeEventListener('focus', refreshOnWake);
      window.removeEventListener('online', refreshOnWake);
    };
  }, [signed]);

  const updateUser = (userUpdated: UserResponse): void => {
    setUser(userUpdated);
    localStorage.setItem(USER_DATA, JSON.stringify(userUpdated));
  };

  const contextValue: AuthContextData = useMemo(() => ({
    signed,
    loading,
    user,
    checkCurrentAuthUser,
    signIn,
    signOut,
    register,
    isAdmin,
    updateUser
  }), [signed, loading, user, checkCurrentAuthUser, signIn, signOut, register, isAdmin, updateUser]);

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthProvider;
