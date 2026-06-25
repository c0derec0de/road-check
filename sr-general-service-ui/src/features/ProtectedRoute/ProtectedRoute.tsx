import { Navigate } from 'react-router-dom';
import { authUtils, type AuthRole } from '../../shared/lib/auth';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: AuthRole[];
  redirectTo?: string;
}

export function ProtectedRoute({
  children,
  allowedRoles,
  redirectTo = '/login',
}: ProtectedRouteProps) {
  if (!authUtils.hasAccess()) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles?.length && !authUtils.hasRole(allowedRoles)) {
    return <Navigate to={redirectTo} replace />;
  }

  return <>{children}</>;
}

