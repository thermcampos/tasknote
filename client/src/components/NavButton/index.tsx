import React from 'react';

/**
 * Props for the NavButton component
 * @interface Props
 */
interface Props {
  children: React.ReactElement;
  className: string;
  onClick: () => void;
  title?: string;
}

/**
 * NavButton component that looks like a link but behaves like a button
 *
 * @param {React.PropsWithChildren<Props>} props - The component props
 * @returns {React.ReactElement} A styled anchor tag that prevents default navigation
 *
 * @example
 * <NavButton className="nav-item" onClick={() => handleLogout()}>
 *   <span>Logout</span>
 * </NavButton>
 */
function NavButton(props: React.PropsWithChildren<Props>): React.ReactElement {
  return (
    <a
      href="#"
      className={props.className}
      title={props.title}
      onClick={(e) => {
        e.preventDefault();
        props.onClick();
      }}
    >
      {props.children}
    </a>
  );
};

export default NavButton;
