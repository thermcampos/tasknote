import React, { useContext, useEffect, useState } from 'react';
import { Nav } from 'react-bootstrap';
import { NavLink } from 'react-router';
import { useTranslation } from 'react-i18next';
import AuthContext from '../../context/AuthContext';
import SidebarContext from '../../context/SidebarContext';
import NavButton from '../NavButton';
import { env } from '../../env';
import './style.scss';
import { BoxArrowRight, InfoCircleFill, PersonFill, StarFill } from 'react-bootstrap-icons';

interface Props {
  isMobileOpen: boolean;
  setIsMobileOpen: (isOpen: boolean) => void;
}

/**
 * Sidebar component renders the sidebar navigation menu.
 *
 * @returns {React.ReactNode} The rendered Sidebar component.
 */
function Sidebar(props: React.PropsWithChildren<Props>): React.ReactNode {
  const { signOut, user } = useContext(AuthContext);
  const { currentPage, setNewPage } = useContext(SidebarContext);
  const [lastSeen, setLastSeen] = useState('');
  const { t } = useTranslation();
  const build = `Build: ${env.VITE_BUILD}`;
  const changeLogUrl = 'https://lightroasted.vps-kinghost.net/rmcampos/tasknote/src/branch/main/CHANGELOG.md';

  // Note: when selected, change class to plus-jakarta-sans-thin and add background

  const logout = (): void => {
    setNewPage('/home');
    signOut();
  };

  const isHomeSelected = (): string => {
    const isHomeSelection: boolean = currentPage === '/home'
      || currentPage == '/tasks/new'
      || currentPage.includes('/tasks/edit')
      || currentPage == '/notes/new'
      || currentPage.includes('/notes/edit');

    if (isHomeSelection) {
      return 'selected';
    }

    return '';
  };

  useEffect(() => {
    if (user && user.lastLogin) {
      const utcString = user.lastLogin.endsWith('Z') ? user.lastLogin : `${user.lastLogin}Z`;
      const date = new Date(utcString);
      if (Number.isNaN(date.getTime())) {
        setLastSeen('');
        return;
      }
      const fmtted = date.toLocaleString(navigator.language, {
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
      setLastSeen(fmtted);
    }
  }, [user]);

  return (
    <>
      <button
        className="d-lg-none position-fixed top-0 start-0 btn btn-light m-2 z-3"
        onClick={() => props.setIsMobileOpen(!props.isMobileOpen)}
        aria-label="Toggle sidebar"
      >
        <i className="bi bi-list"></i>
      </button>

      <div className={`d-flex flex-column vh-100 sidebar ${props.isMobileOpen ? 'sidebar-mobile-open' : ''}`}>
        <div className="sidebar-header plus-jakarta-sans-bold">
          <img src={`https://gravatar.com/avatar/${user?.gravatarImageUrl}.jpg`} alt="User icon" />
          <span className="header-username">{user?.name ? user?.name : 'User'}</span>
        </div>

        <div className="header-spacer"></div>

        <Nav className="flex-column p-3 plus-jakarta-sans-thin">
          <NavLink to="/home" className="mb-2" onClick={() => setNewPage('/home')}>
            <div className={`sidebar-nav ${isHomeSelected()}`}>
              <StarFill />
              Home
            </div>
          </NavLink>
          <NavLink to="/account" className="mb-2" onClick={() => setNewPage('/account')}>
            <div className={`sidebar-nav ${currentPage === '/account' ? 'selected' : ''}`}>
              <PersonFill />
              {t('footer_my_account')}
            </div>
          </NavLink>
          <NavLink to="/about" className="mb-2" onClick={() => setNewPage('/about')}>
            <div className={`sidebar-nav ${currentPage === '/about' ? 'selected' : ''}`}>
              <InfoCircleFill />
              {t('home_nav_about')}
            </div>
          </NavLink>
          <NavButton className="mb-2" onClick={() => logout()}>
            <div className="sidebar-nav">
              <BoxArrowRight />
              {t('logout')}
            </div>
          </NavButton>
        </Nav>

        {/* Footer at the bottom */}
        <div className="mt-auto text-center text-muted py-3">
          {lastSeen && (
            <div>
              <small>{t('sidebar_last_seen', { time: lastSeen })}</small>
            </div>
          )}
          <a
            data-testid="footer-text"
            href={changeLogUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="footer-link"
          >
            <small>{build}</small>
          </a>
        </div>
      </div>

      {props.isMobileOpen && (
        <button
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-50 d-lg-none"
          style={{ zIndex: 1 }}
          onClick={() => props.setIsMobileOpen(false)}
          aria-label="Close mobile menu"
        />
      )}
    </>
  );
}

export default Sidebar;
