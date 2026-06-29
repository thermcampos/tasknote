import React, { useEffect, useMemo, useState } from 'react';
import AuthContext, { AuthContextData } from './AuthContext';
import { API_TOKEN, REDIRECT_PATH, USER_DATA } from '../app-constants/app-constants';
import { SignInResponse } from '../types/SigninResponse';
import api from '../api-service/api';
import ApiConfig from '../api-service/apiConfig';
import { UserResponse } from '../types/UserResponse';
import { UserRegistration } from '../types/UserRegistration';

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
      // Clear stored client id and name
      localStorage.clear();
      localStorage.setItem(REDIRECT_PATH, pathname);
      setUser(undefined);
      setSigned(false);
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
      const currentUser: UserResponse = {
        userId: registerResponse.userId,
        name: registerResponse.name,
        email: registerResponse.email,
        admin: registerResponse.admin,
        createdAt: new Date(registerResponse.createdAt),
        gravatarImageUrl: registerResponse.gravatarImageUrl,
        lang: registerResponse.lang,
        lastLogin: registerResponse.lastLogin
      };

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
  };

  useEffect(() => {
    checkCurrentAuthUser(window.location.pathname)
      .catch(e => console.error(e))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!signed) return;
    const TWENTY_FIVE_MINUTES = 25 * 60 * 1000;
    const intervalId = setInterval(() => {
      checkCurrentAuthUser(window.location.pathname).catch(() => {
        setSigned(false);
        setUser(undefined);
        localStorage.clear();
      });
    }, TWENTY_FIVE_MINUTES);
    return () => clearInterval(intervalId);
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
