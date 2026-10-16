/// <reference types="vite/client" />
import { useEffect, useRef, useState } from 'react';

/** Coordinates are WGS84 GPS coordinates for the *issue*, not the reporter. */
export interface ConfirmedLocation {
  address: string;
  latitude: number | null;
  longitude: number | null;
}

type Point = { lat: number; lng: number };
type PlacePrediction = {
  text: { toString(): string };
  toPlace(): {
    fetchFields(options: { fields: string[] }): Promise<void>;
    formattedAddress?: string;
    location?: { lat(): number; lng(): number };
  };
};

type MapsApi = {
  maps: { importLibrary(name: string): Promise<any> }; // Loaded from Google at runtime.
};
declare global {
  interface Window {
    google?: MapsApi;
    __civicMapsReady?: () => void;
  }
}

// This Vite variable is a *public browser key*: restrict it by website + APIs.
const key = (import.meta.env.VITE_GOOGLE_MAPS_API_KEY ?? '').trim();
const mapId = (import.meta.env.VITE_GOOGLE_MAPS_MAP_ID ?? 'DEMO_MAP_ID').trim() || 'DEMO_MAP_ID';
export const mapsConfigured = key.length > 0;

let loader: Promise<MapsApi> | null = null;
function getMaps(): Promise<MapsApi> {
  if (typeof window === 'undefined') return Promise.reject(new Error('Maps requires a browser.'));
  if (window.google?.maps?.importLibrary) return Promise.resolve(window.google);
  if (!key) return Promise.reject(new Error('Google Maps has not been configured.'));
  if (loader) return loader;
  loader = new Promise<MapsApi>((resolve, reject) => {
    const script = document.createElement('script');
    script.async = true;
    script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(key)}&v=weekly&loading=async&callback=__civicMapsReady`;
    window.__civicMapsReady = () => {
      delete window.__civicMapsReady;
      if (window.google?.maps?.importLibrary) resolve(window.google);
      else reject(new Error('Google Maps did not initialize.'));
    };
    script.onerror = () => {
      delete window.__civicMapsReady;
      loader = null;
      reject(new Error('Could not load Google Maps. Check your API key, billing, API permissions, and connection.'));
    };
    document.head.append(script);
  });
  return loader;
}

const initialCenter: Point = { lat: -25.7479, lng: 28.2293 }; // Initial view only; not a saved location.
function pointOf(position: { lat: number | (() => number); lng: number | (() => number) } | null | undefined): Point | null {
  if (!position) return null;
  const lat = typeof position.lat === 'function' ? position.lat() : position.lat;
  const lng = typeof position.lng === 'function' ? position.lng() : position.lng;
  return Number.isFinite(lat) && Number.isFinite(lng) ? { lat, lng } : null;
}
export function isValidPoint(latitude: number | null | undefined, longitude: number | null | undefined): boolean {
  return typeof latitude === 'number' && Number.isFinite(latitude) && latitude >= -90 && latitude <= 90 &&
    typeof longitude === 'number' && Number.isFinite(longitude) && longitude >= -180 && longitude <= 180;
}

interface PickerProps {
  value: ConfirmedLocation;
  onChange(next: ConfirmedLocation): void;
}

/** Interactive address search and draggable marker, only rendered on Requester submission. */
export function LocationPicker({ value, onChange }: PickerProps) {
  const mapEl = useRef<HTMLDivElement>(null);
  const map = useRef<any>(null);
  const marker = useRef<any>(null);
  const googleRef = useRef<MapsApi | null>(null);
  const latestChoice = useRef(0);
  const latestQuery = useRef(0);
  const sessionToken = useRef<any>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  const [ready, setReady] = useState(false);
  const [loading, setLoading] = useState(mapsConfigured);
  const [error, setError] = useState('');
  const [suggestions, setSuggestions] = useState<PlacePrediction[]>([]);
  const [searching, setSearching] = useState(false);
  const [locating, setLocating] = useState(false);

  const confirmed = isValidPoint(value.latitude, value.longitude);
  const putMarker = (point: Point, zoom = 17) => {
    if (!map.current || !marker.current) return;
    marker.current.position = point;
    map.current.setCenter(point);
    map.current.setZoom(zoom);
  };

  async function placePin(point: Point) {
    const choice = ++latestChoice.current;
    setSuggestions([]);
    putMarker(point);
    // Never lose a valid coordinate if reverse-geocoding is unavailable.
    const fallback = `Pinned location (${point.lat.toFixed(6)}, ${point.lng.toFixed(6)})`;
    onChangeRef.current({ address: fallback, latitude: point.lat, longitude: point.lng });
    try {
      const geoLibrary = await googleRef.current?.maps.importLibrary('geocoding');
      if (!geoLibrary) return;
      const response = await new geoLibrary.Geocoder().geocode({ location: point });
      const address: string | undefined = response.results?.[0]?.formatted_address;
      if (choice === latestChoice.current && address) {
        onChangeRef.current({ address, latitude: point.lat, longitude: point.lng });
      }
    } catch {
      // Pin is still confirmed; keep the exact coordinates and a useful fallback label.
    }
  }

  useEffect(() => {
    if (!mapsConfigured || !mapEl.current) return;
    let mounted = true;
    let listener: { remove(): void } | null = null;
    setLoading(true);
    void (async () => {
      try {
        const api = await getMaps();
        const [mapLibrary, markerLibrary, placesLibrary] = await Promise.all([
          api.maps.importLibrary('maps'),
          api.maps.importLibrary('marker'),
          api.maps.importLibrary('places'),
        ]);
        if (!mounted || !mapEl.current) return;
        googleRef.current = api;
        sessionToken.current = new placesLibrary.AutocompleteSessionToken();
        const starting = isValidPoint(value.latitude, value.longitude)
          ? { lat: value.latitude!, lng: value.longitude! } : initialCenter;
        const instance = new mapLibrary.Map(mapEl.current, {
          center: starting, zoom: starting === initialCenter ? 12 : 17,
          mapId, streetViewControl: false, mapTypeControl: false, fullscreenControl: true,
        });
        const pin = new markerLibrary.AdvancedMarkerElement({
          map: instance, position: isValidPoint(value.latitude, value.longitude) ? starting : null,
          title: 'Exact issue location (drag to adjust)', gmpDraggable: true,
        });
        map.current = instance;
        marker.current = pin;
        listener = instance.addListener('click', (event: { latLng?: { lat(): number; lng(): number } }) => {
          const point = pointOf(event.latLng);
          if (point) void placePin(point);
        });
        pin.addEventListener('gmp-dragend', () => {
          const point = pointOf(pin.position);
          if (point) void placePin(point);
        });
        setReady(true);
        setError('');
      } catch (e) {
        if (mounted) setError(e instanceof Error ? e.message : 'Maps could not load.');
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => {
      mounted = false;
      listener?.remove();
      if (marker.current) marker.current.map = null;
      marker.current = null;
      map.current = null;
    };
  // Map is created only once; subsequent selections update marker separately.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!ready || !marker.current) return;
    if (isValidPoint(value.latitude, value.longitude)) {
      marker.current.position = { lat: value.latitude, lng: value.longitude };
    } else {
      marker.current.position = null;
    }
  }, [ready, value.latitude, value.longitude]);

  useEffect(() => {
    const seq = ++latestQuery.current;
    if (!ready || confirmed || value.address.trim().length < 3) {
      setSuggestions([]);
      setSearching(false);
      return;
    }
    setSearching(true);
    const timer = window.setTimeout(async () => {
      try {
        const library = await googleRef.current?.maps.importLibrary('places');
        if (!library || !sessionToken.current) return;
        const { suggestions: matches } = await library.AutocompleteSuggestion.fetchAutocompleteSuggestions({
          input: value.address.trim(), sessionToken: sessionToken.current,
        });
        if (seq === latestQuery.current) setSuggestions(
          matches.map((s: { placePrediction?: PlacePrediction }) => s.placePrediction).filter(Boolean) as PlacePrediction[]
        );
      } catch {
        if (seq === latestQuery.current) setError('Address suggestions are unavailable. You can still choose a point directly on the map.');
      } finally {
        if (seq === latestQuery.current) setSearching(false);
      }
    }, 300);
    return () => window.clearTimeout(timer);
  }, [value.address, ready, confirmed]);

  async function choose(prediction: PlacePrediction) {
    const choice = ++latestChoice.current;
    ++latestQuery.current;
    setSuggestions([]);
    setSearching(false);
    setLoading(true);
    try {
      const place = prediction.toPlace();
      await place.fetchFields({ fields: ['formattedAddress', 'location'] });
      const point = pointOf(place.location);
      if (!point) throw new Error('This suggestion has no mappable position. Choose another location.');
      if (choice !== latestChoice.current) return;
      putMarker(point);
      onChangeRef.current({
        address: (place.formattedAddress || prediction.text.toString()).slice(0, 500),
        latitude: point.lat, longitude: point.lng,
      });
      const lib = await googleRef.current?.maps.importLibrary('places');
      if (lib) sessionToken.current = new lib.AutocompleteSessionToken();
      setError('');
    } catch (e) {
      if (choice === latestChoice.current) setError(e instanceof Error ? e.message : 'Could not load that address.');
    } finally {
      setLoading(false);
    }
  }

  function currentLocation() {
    if (!navigator.geolocation) { setError('This browser does not support device location.'); return; }
    setLocating(true);
    setError('');
    navigator.geolocation.getCurrentPosition(
      position => { setLocating(false); void placePin({ lat: position.coords.latitude, lng: position.coords.longitude }); },
      () => { setLocating(false); setError('Location permission denied or unavailable. Search an address or click the map instead.'); },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    );
  }

  return <section className="location-picker" aria-label="Issue location selection">
    <label htmlFor="issue-location-input">Location / Address <b>*</b></label>
    <div className="location-search">
      <input id="issue-location-input" autoComplete="off" value={value.address} maxLength={500}
        required aria-autocomplete={mapsConfigured ? 'list' : undefined}
        aria-controls={suggestions.length ? 'location-suggestions' : undefined}
        placeholder="Enter a street, address, suburb or landmark"
        onChange={e => {
          ++latestChoice.current;
          onChange({ address: e.target.value, latitude: null, longitude: null });
        }} />
      {searching && <small className="location-spinner">Searching…</small>}
      {suggestions.length > 0 && <ul id="location-suggestions" role="listbox" className="location-suggestions">
        {suggestions.map((prediction, i) => <li key={`${prediction.text.toString()}-${i}`} role="option" aria-selected="false">
          <button type="button" className="location-suggestion" onClick={() => void choose(prediction)}>{prediction.text.toString()}</button>
        </li>)}
      </ul>}
    </div>
    {mapsConfigured ? <>
      <div className="location-actions">
        <button type="button" className="secondary" onClick={currentLocation} disabled={!ready || locating}>
          {locating ? 'Locating…' : '◎ Use my current location'}
        </button>
        <small>{confirmed ? '✓ Exact issue pin selected — drag it to refine.' : 'Choose a suggestion, tap the map, or drag the pin after selecting an address.'}</small>
      </div>
      <div className="location-map" ref={mapEl} role="group" aria-label="Interactive Google Map. Click or drag pin to set issue location" />
      {loading && <small role="status">Loading Google Maps…</small>}
      {error && <p className="location-error" role="alert">{error}</p>}
      {confirmed && <div className="location-coordinates">Confirmed coordinates: {value.latitude!.toFixed(6)}, {value.longitude!.toFixed(6)}
        <button type="button" className="link" onClick={() => onChange({ address: value.address, latitude: null, longitude: null })}>Clear pin</button>
      </div>}
      {!confirmed && <p className="location-hint">A precise map pin is required to submit this request. The map starts in Pretoria for convenience; no location is saved until you choose one.</p>}
    </> : <p className="location-hint" role="status">Address autocomplete and the map are unavailable until a Google Maps browser key is configured. You may enter an address manually for local testing; no coordinates will be saved.</p>}
  </section>;
}

/** Shared read-only exact issue pin for Requester, Staff, and Manager. */
export function SavedLocationMap({ location, latitude, longitude }: { location?: string | null; latitude?: number | null; longitude?: number | null }) {
  const mapEl = useRef<HTMLDivElement>(null);
  const valid = isValidPoint(latitude, longitude);
  const [error, setError] = useState('');
  useEffect(() => {
    if (!valid || !mapsConfigured || !mapEl.current) return;
    let mounted = true;
    let pin: any = null;
    void (async () => {
      try {
        const api = await getMaps();
        const [{ Map }, { AdvancedMarkerElement }] = await Promise.all([
          api.maps.importLibrary('maps'), api.maps.importLibrary('marker'),
        ]);
        if (!mounted || !mapEl.current) return;
        const pos = { lat: latitude!, lng: longitude! };
        const map = new Map(mapEl.current, {
          center: pos, zoom: 17, mapId, streetViewControl: false, mapTypeControl: false,
        });
        pin = new AdvancedMarkerElement({ map, position: pos, title: 'Saved issue location', gmpDraggable: false });
      } catch (e) {
        if (mounted) setError(e instanceof Error ? e.message : 'Could not display the map.');
      }
    })();
    return () => { mounted = false; if (pin) pin.map = null; };
  }, [valid, latitude, longitude]);

  if (!valid) return <p className="muted">No exact map pin was recorded for this request.</p>;
  const directions = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${latitude},${longitude}`)}`;
  return <div className="saved-location">
    {mapsConfigured && <div ref={mapEl} className="location-map location-map-small" role="img" aria-label={`Map of issue at ${location || 'saved coordinates'}`} />}
    {error && <p role="alert" className="location-error">{error}</p>}
    <div className="location-coordinates">
      <small>Exact issue location: {latitude!.toFixed(6)}, {longitude!.toFixed(6)}</small>
      <a href={directions} target="_blank" rel="noopener noreferrer">Open location in Google Maps ↗</a>
    </div>
  </div>;
}
