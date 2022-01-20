import React, {useImperativeHandle} from 'react';
import {UIManager, findNodeHandle} from 'react-native';
import StreamCameraView from './StreamCameraView';

const StreamCamera = ({onStatus, ...props}, ref) => {
  const cameraRef = React.useRef();

  const _onChange = event => {
    if (!onStatus) {
      return;
    }
    onStatus(event.nativeEvent.code, event.nativeEvent.msg);
  };

  const dispatch = (fn, ...args) => {
    const tag = findNodeHandle(cameraRef.current);
    UIManager.dispatchViewManagerCommand(tag, fn, [...args]);
  };

  const startPreview = React.useCallback(() => dispatch('startprev'), []);
  const stopPreview = React.useCallback(() => dispatch('stopprev'), []);
  const start = React.useCallback(() => dispatch('start'), []);
  const stop = React.useCallback(() => dispatch('stop'), []);
  const switchCamera = React.useCallback(() => dispatch('switchCamera'), []);
  const flashEnable = React.useCallback(
    enable => dispatch('flashEnable', !!enable),
    [],
  );

  useImperativeHandle(
    ref,
    () => ({startPreview, stopPreview, start, stop, switchCamera, flashEnable}),
    [startPreview, stopPreview, start, stop, switchCamera, flashEnable],
  );

  return <StreamCameraView {...props} ref={cameraRef} onChange={_onChange} />;
};

module.exports= React.forwardRef(StreamCamera);
