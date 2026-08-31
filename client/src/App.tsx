import React, { useContext, useEffect, useMemo, useState } from 'react';
import {
  createBrowserRouter,
  Navigate,
  RouteObject,
  RouterProvider
} from 'react-router';
import AuthContext from './context/AuthContext';
import BrowserRoutes from './routes';
import ProtectedRoute from './routes/ProtectedRoute';
import PrivateLayout from './layout/PrivateLayout';
import Landing from './views/Landing';
import Login from './views/Login';
import NotFound from './views/NotFound';
import Register from './views/Register';
import EmailConfirmation from './views/EmailConfirmation';
import ResetPassword from './views/ResetPassword';
import CompleteResetPassword from './views/CompleteResetPassword';
import SharedNote from './views/SharedNote';
import api from './api-service/api';
import ApiConfig from './api-service/apiConfig';
import { THEME_PENDING } from './app-constants/app-constants';
import { UserPatchRequest } from './types/UserPatchRequest';
import './styles/custom.scss';

/**
 * Routes for the users who are not signed in.
 * @type {RouteObject[]}
 */
const notSignedRouter: RouteObject[] = [
  {
    path: '/',
    element: <Landing />
  },
  {
    path: '/login',
    element: <Login />
  },
  {
    path: '/register',
    element: <Register />
  },
  {
    path: '/home',
    element: <Navigate to="/login" replace />
  },
  {
    path: '/email-confirmation',
    element: <EmailConfirmation />
  },
  {
    // The reset-password is where the password reset workflow starts
    path: '/reset-password',
    element: <ResetPassword />
  },
  {
    path: '/finish-reset-password',
    element: <CompleteResetPassword />
  },
  {
    path: '/public/notes/:token',
    element: <SharedNote />
  },
  {
    path: '*',
    element: <Navigate to="/" replace />
  }
];

/**
 * Routes for users who are signed in.
 * @type {RouteObject[]}
 */
const signedRouter: RouteObject[] = [
  {
    path: '/',
    element: <ProtectedRoute />,
    children: [
      {
        element: <PrivateLayout />,
        children: BrowserRoutes
      }
    ]
  },
  {
    path: '/public/notes/:token',
    element: <SharedNote />
  },
  {
    path: '*',
    element: <NotFound />
  }
];

/**
 * The main application component that sets up routing based on the
 * user's authentication status.
 *
 * @component
 * @returns {React.ReactNode} The rendered component.
 */
function App(): React.ReactNode {
  const { signed, loading, user } = useContext(AuthContext);
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('theme') ?? 'light';
  });

  const browserRouter = useMemo(() => {
    if (signed) {
      return createBrowserRouter(signedRouter);
    }
    return createBrowserRouter(notSignedRouter);
  }, [signed]);

  const patchTheme = async (newTheme: string): Promise<void> => {
    const payload: UserPatchRequest = {
      name: null,
      email: null,
      password: null,
      passwordAgain: null,
      lang: null,
      theme: newTheme
    };
    try {
      await api.patchJSON(ApiConfig.userUrl, payload);
      localStorage.removeItem(THEME_PENDING);
    }
    catch {
      localStorage.setItem(THEME_PENDING, newTheme);
    }
  };

  useEffect(() => {
    document.body.setAttribute('data-bs-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  useEffect(() => {
    if (!user) {
      return;
    }
    const pendingTheme = localStorage.getItem(THEME_PENDING);
    if (pendingTheme) {
      patchTheme(pendingTheme);
      return;
    }
    setTheme(user.theme ?? 'light');
  }, [user]);

  const toggleTheme = () => {
    const newTheme = theme === 'light' ? 'dark' : 'light';
    setTheme(newTheme);
    if (signed) {
      patchTheme(newTheme);
    }
  };

  return (
    <>
      <button
        type="button"
        className="btn btn-dark"
        onClick={toggleTheme}
      >
        {theme === 'dark' ? '☀ Light Mode' : '🌙 Dark Mode'}
      </button>

      {/* Your routes or content here */}
      {!loading && <RouterProvider router={browserRouter} />}
    </>
  );
}

export default App;
