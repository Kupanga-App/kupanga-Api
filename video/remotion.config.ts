import {Config} from '@remotion/cli/config';

Config.setVideoImageFormat('jpeg');
Config.setJpegQuality(95);
// Captures 4K (3840 × 2160) : quelques images en mémoire à la fois suffisent
Config.setConcurrency(4);
