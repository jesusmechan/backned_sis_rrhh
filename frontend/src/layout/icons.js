import {
  BookOpen,
  ClipboardCheck,
  Clock3,
  FileSpreadsheet,
  FileText,
  Fingerprint,
  GitBranch,
  Home,
  Inbox,
  List,
  LogIn,
  Shield,
  Star,
  User,
  UserCog,
  UserPlus,
  Users,
  Wallet,
  Library
} from 'lucide-react';

const ICONS = {
  Home,
  Inbox,
  ClipboardCheck,
  Clock3,
  LogIn,
  Fingerprint,
  User,
  Users,
  UserCog,
  GitBranch,
  FileSpreadsheet,
  FileText,
  Shield,
  List,
  Wallet,
  BookOpen,
  Star,
  UserPlus,
  Library
};

export function menuIcon(name) {
  return ICONS[name] || Home;
}

export const MENU_ICON_OPTIONS = Object.keys(ICONS);
