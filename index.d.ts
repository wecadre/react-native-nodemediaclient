declare module "react-native-nodemediaclient" {
  import type * as React from "react";
  import { ViewProps } from "react-native";

  export interface NodeCameraViewProps extends ViewProps {
    ref?: any;
    outputUrl?: string;
    camera?: CameraConfig;
    audio?: AudioConfig;
    video?: VideoConfig;
    autopreview?: boolean;
    denoise?: boolean;
    dynamicRateEnable?: boolean;
    smoothSkinLevel?: SmoothSkinLevel;
    cryptoKey?: string;
    onStatus?(code?: OutputStreamStatus, status?: string): any;
  }

  export interface NodeCameraViewType {
    /** Stop streaming */
    stop(): any;
    /** Start streaming */
    start(): any;
    /** Switch front or back camera */
    switchCamera(): any;
    /** Enable or disable flash */
    // eslint-disable-next-line no-unused-vars
    flashEnable(enable?: boolean): any;
    /** Start camera preview */
    startPreview(): any;
    /** Stop camera preview */
    stopPreview(): any;
  }

  export interface VideoConfig {
    preset?: number;
    bitrate?: number;
    profile?: Profile;
    fps?: FPS;
    videoFrontMirror?: boolean;
  }

  export interface AudioConfig {
    bitrate?: number;
    profile?: Profile;
    samplerate?: AudioSampleRate;
  }

  export interface CameraConfig {
    cameraId?: CameraId;
    cameraFrontMirror?: boolean;
  }

  type CameraId = 0 | 1;
  type Profile = 0 | 1 | 2;
  type AudioSampleRate = 8000 | 16000 | 32000 | 44100 | 48000;
  type FPS = 15 | 20 | 24 | 30;
  type SmoothSkinLevel = 0 | 1 | 2 | 3 | 4 | 5;

  export enum OutputStreamStatus {
    Connecting = 2000,
    Start = 2001,
    Failed = 2002,
    Closed = 2004,
    None,
  }
  export enum InputStreamStatus {
    Connecting = 1000,
    Connected = 1001,
    Buffering = 1101,
    BufferFull = 1102,
    Resolution = 1104,
  }
  export interface NodePlayerViewProps extends ViewProps {
    ref: any;
    inputUrl: string;
    bufferTime?: number;
    maxBufferTime?: number;
    autoplay?: boolean;
    audioEnable?: boolean;
    scaleMode?: "ScaleToFill" | "ScaleAspectFit" | "ScaleAspectFill";
    renderType?: "SURFACEVIEW" | "TEXTUREVIEW";
    cryptoKey?: string;
    onStatus?(code?: InputStreamStatus, status?: string): any;
  }

  export interface NodePlayerViewType {
    /** Pause video */
    pause(): any;
    /** Stop video */
    stop(): any;
    /** Start video */
    start(): any;
  }

  export const NodePlayerView: React.ForwardRefRenderFunction<
    NodePlayerViewType,
    NodePlayerViewProps
  >;

  export const NodeCameraView: React.ForwardRefRenderFunction<
    NodeCameraViewType,
    NodeCameraViewProps
  >;
}
