import React, { useEffect, useMemo, useState } from 'react';
import SidebarContext, { SidebarContextData } from './SidebarContext';

const SIDEBAR_COLLAPSED_KEY = 'SIDEBAR_COLLAPSED';

interface Props {
  children: React.ReactNode;
}

const SidebarProvider: React.FC<{ children: React.ReactNode }> = ({ children }: Props) => {
  const [currentPage, setCurrentPage] = useState<string>('/home');
  const [isCollapsed, setIsCollapsed] = useState<boolean>(
    () => localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true'
  );

  useEffect(() => {
    localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(isCollapsed));
  }, [isCollapsed]);

  const setNewPage = (page: string): void => {
    setCurrentPage(page);
  };

  const toggleCollapsed = (): void => {
    setIsCollapsed(prev => !prev);
  };

  const contextValue: SidebarContextData = useMemo(() => ({
    currentPage,
    setNewPage,
    isCollapsed,
    toggleCollapsed
  }), [currentPage, setNewPage, isCollapsed, toggleCollapsed]);

  return (
    <SidebarContext.Provider value={contextValue}>
      { children }
    </SidebarContext.Provider>
  );
};

export default SidebarProvider;
