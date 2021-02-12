import React from "react";
import {
  requireNativeComponent,
  ViewProps,
  UIManager,
  findNodeHandle,
} from "react-native";

const NodeCameraView = (props, ref) => {
  const videoRef = React.useRef();
  const _onChange = (event) => {
    if (!props.onStatus) {
      return;
    }
    props.onStatus(event.nativeEvent.code, event.nativeEvent.msg);
  };

  const switchCamera = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(videoRef.current),
      UIManager.getViewManagerConfig("RCTNodeCamera").Commands.switchCamera,
      null
    );
  };

  const flashEnable = (enable) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(videoRef.current),
      UIManager.getViewManagerConfig("RCTNodeCamera").Commands.flashEnable,
      [enable]
    );
  };

  const startPreview = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(videoRef.current),
      UIManager.getViewManagerConfig("RCTNodeCamera").Commands.startprev,
      null
    );
  };

  const stopPreview = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(videoRef.current),
      UIManager.getViewManagerConfig("RCTNodeCamera").Commands.stopprev,
      null
    );
  };

  const start = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(videoRef.current),
      UIManager.getViewManagerConfig("RCTNodeCamera").Commands.start,
      null
    );
  };

  const stop = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(videoRef.current),
      UIManager.getViewManagerConfig("RCTNodeCamera").Commands.stop,
      null
    );
  };
  React.useImperativeHandle(
    ref,
    () => ({
      stop,
      start,
      switchCamera,
      flashEnable,
      startPreview,
      stopPreview,
    }),
    [switchCamera, stop, start, flashEnable, startPreview, stopPreview]
  );

  return <RCTNodeCamera {...props} ref={videoRef} onChange={_onChange} />;
};

const RCTNodeCamera = requireNativeComponent("RCTNodeCamera");

module.exports = React.forwardRef(NodeCameraView);
