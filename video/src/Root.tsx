import {Composition} from 'remotion';
import {KupangaVideo} from './Video';
import {FPS, charger, defaultTimings, type Donnees} from './timing';

const initial: Donnees = {scenes: defaultTimings(), hasVoice: false, manifeste: {}, tailles: {}, whatsapp: {}};
const total = (d: Donnees) => d.scenes.reduce((n, s) => n + s.frames, 0);

/** Composition en 1920 × 1080 ; rendu final en 4K avec --scale=2 (captures en 3840 px : nettes). */
export const Root: React.FC = () => (
  <Composition
    id="Kupanga"
    component={KupangaVideo}
    width={1920}
    height={1080}
    fps={FPS}
    durationInFrames={total(initial)}
    defaultProps={initial}
    calculateMetadata={async () => {
      const props = await charger();
      return {props, durationInFrames: total(props)};
    }}
  />
);
