import {loadFont as loadSerif} from '@remotion/google-fonts/DMSerifDisplay';
import {loadFont as loadSans} from '@remotion/google-fonts/DMSans';

/** Charte de Kupanga (front et back-office) : Night, Olive, Mint ; DM Serif Display et DM Sans. */
export const C = {
  night: '#10212B',
  nightDeep: '#0A161D',
  nightSoft: '#1A303C',
  olive: '#8FA464',
  oliveLight: '#B5C78F',
  mint: '#EFFBDB',
  paper: '#F4F4EF',
  white: '#FFFFFF',
  muted: 'rgba(239, 251, 219, 0.62)',
};

export const serif = loadSerif('normal', {weights: ['400'], subsets: ['latin', 'latin-ext']}).fontFamily;
export const sans = loadSans('normal', {weights: ['400', '500', '700'], subsets: ['latin', 'latin-ext']}).fontFamily;
