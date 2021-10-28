import React from 'react';
import PropTypes from 'prop-types';
import {Button, Checkbox, Modal, Input} from 'antd';
import electron from 'electron';
import './configuration.css';
import writeWebDavConfiguration from '../../../write-webdav-configuration';
import copyPingConfiguration from '../../models/file-systems/copy-ping-configuration';

class Configuration extends React.Component {
  state = {
    server: undefined,
    password: undefined,
    username: undefined,
    ignoreCertificateErrors: false,
    modified: false,
    version: undefined,
    pingAfterCopy: false,
    maxWaitSeconds: undefined,
    pingTimeoutSeconds: undefined,
    pending: false,
    diagnoseState: undefined,
    diagnoseFile: undefined
  };
  componentDidMount() {
    this.updateSettings();
  }

  componentDidUpdate(prevProps, prevState, snapshot) {
    if (prevProps.visible !== this.props.visible && this.props.visible) {
      this.updateSettings();
    }
  }

  updateSettings = () => {
    const cfg = electron.remote.getGlobal('webdavClient');
    const {name} = electron.remote.getGlobal('settings') || {};
    const {config: webdavClientConfig = {}} = cfg || {};
    let {
      maxWaitSeconds = copyPingConfiguration.maxWaitSeconds,
      pingTimeoutSeconds = copyPingConfiguration.pingTimeoutSeconds
    } = webdavClientConfig;
    if (
      Number.isNaN(Number(maxWaitSeconds)) ||
      Number.isNaN(Number(pingTimeoutSeconds)) ||
      Number(maxWaitSeconds) <= 0
    ) {
      maxWaitSeconds = 0;
      pingTimeoutSeconds = 0;
    }
    if (Number(maxWaitSeconds) > 0 && Number(pingTimeoutSeconds) > Number(maxWaitSeconds)) {
      pingTimeoutSeconds = maxWaitSeconds;
    }
    const pingAfterCopy = !Number.isNaN(Number(maxWaitSeconds)) &&
      !Number.isNaN(Number(pingTimeoutSeconds)) &&
      Number(maxWaitSeconds) > 0;
    this.setState({
      server: webdavClientConfig.server,
      password: webdavClientConfig.password,
      username: webdavClientConfig.username,
      ignoreCertificateErrors: webdavClientConfig.ignoreCertificateErrors,
      modified: false,
      version: webdavClientConfig.version,
      name,
      pingAfterCopy,
      maxWaitSeconds,
      pingTimeoutSeconds,
      pending: false,
      diagnoseState: undefined,
      diagnoseFile: undefined
    });
  };

  onSettingChanged = (settingName) => (e) => {
    this.setState({
      [settingName]: e.target.value,
      modified: true,
    });
    const cfg = electron.remote.getGlobal('webdavClient');
    cfg.config[settingName] = e.target.value;
    writeWebDavConfiguration(cfg.config);
  };

  onPingAfterCopyChanged = (e) => {
    let {
      maxWaitSeconds,
      pingTimeoutSeconds
    } = copyPingConfiguration;
    const pingAfterCopy = e.target.checked;
    if (!pingAfterCopy) {
      maxWaitSeconds = 0;
      pingTimeoutSeconds = 0;
    }
    const cfg = electron.remote.getGlobal('webdavClient');
    cfg.config.maxWaitSeconds = maxWaitSeconds;
    cfg.config.pingTimeoutSeconds = pingTimeoutSeconds;
    writeWebDavConfiguration(cfg.config);
    this.setState({
      pingAfterCopy,
      maxWaitSeconds,
      pingTimeoutSeconds,
      modified: true
    });
  };

  onIgnoreCertificateErrorsSettingChanged = (e) => {
    this.setState({
      ignoreCertificateErrors: e.target.checked,
      modified: true,
    });
    const cfg = electron.remote.getGlobal('webdavClient');
    cfg.config.ignoreCertificateErrors = e.target.checked;
    writeWebDavConfiguration(cfg.config);
  };

  onClose = () => {
    const {onClose} = this.props;
    if (onClose) {
      onClose(this.state.modified);
    }
  };

  diagnose = () => {
    const {fileSystem} = this.props;
    const {
      server,
      password,
      username,
      ignoreCertificateErrors
    } = this.state;
    if (fileSystem) {
      this.setState({
        pending: true,
        diagnoseFile: undefined
      }, () => {
        const cb = (o) => this.setState({diagnoseState: o});
        fileSystem
          .diagnose(
            {
              server,
              password,
              username,
              ignoreCertificateErrors
            },
            cb
          )
          .then((result) => {
            const {
              filePath
            } = result || {};
            this.setState({
              diagnoseState: undefined,
              pending: false,
              diagnoseFile: filePath
            })
          });
      });
    }
  };

  render () {
    const {
      visible,
      fileSystem
    } = this.props;
    const {
      server,
      username,
      password,
      version,
      name = 'Cloud Data',
      maxWaitSeconds,
      pingTimeoutSeconds,
      pingAfterCopy,
      pending,
      diagnoseState,
      diagnoseFile
    } = this.state;
    return (
      <Modal
        visible={visible}
        title="Configuration"
        footer={null}
        onCancel={this.onClose}
        onClose={this.onClose}
        width="60%"
      >
        <div
          className="configuration"
        >
          <div
            className="row"
          >
            <span className="label">
              Server:
            </span>
            <Input
              className="input"
              value={server}
              onChange={this.onSettingChanged('server')}
            />
            {
              fileSystem && (
                <Button
                  disabled={pending}
                  onClick={this.diagnose}
                  style={{marginLeft: 5}}
                >
                  TEST
                </Button>
              )
            }
          </div>
          <div
            className="row"
          >
            <span className="label">
              User name:
            </span>
            <Input
              className="input"
              value={username}
              onChange={this.onSettingChanged('username')}
            />
          </div>
          <div
            className="row"
          >
            <span className="label">
              Password:
            </span>
            <Input.Password
              className="input"
              value={password}
              onChange={this.onSettingChanged('password')}
            />
          </div>
          <div
            className="row"
          >
            <Checkbox
              checked={this.state.ignoreCertificateErrors}
              onChange={this.onIgnoreCertificateErrorsSettingChanged}
            >
              Ignore certificate errors (re-launch is required)
            </Checkbox>
          </div>
          <div
            className="row"
          >
            <Checkbox
              checked={pingAfterCopy}
              onChange={this.onPingAfterCopyChanged}
            >
              Check destination file existence after copy operation
            </Checkbox>
          </div>
          {
            pingAfterCopy && (
              <div
                className="row"
              >
                <span className="label-small">
                  Ping duration (seconds):
                </span>
                <Input
                  className="input"
                  value={maxWaitSeconds}
                  onChange={this.onSettingChanged('maxWaitSeconds')}
                  size="small"
                />
              </div>
            )
          }
          {
            pingAfterCopy && (
              <div
                className="row"
              >
                <span className="label-small">
                  Ping every (seconds):
                </span>
                <Input
                  className="input"
                  value={pingTimeoutSeconds}
                  onChange={this.onSettingChanged('pingTimeoutSeconds')}
                  size="small"
                />
              </div>
            )
          }
          {
            version && (
              <div className="app-version">
                {name} App Version: <b>{version}</b>
              </div>
            )
          }
          {
            (diagnoseState || diagnoseFile) && (
              <div
                className="row"
                style={{
                  justifyContent: 'space-between',
                  marginTop: 10,
                  paddingTop: 5,
                  borderTop: '1px solid #ccc'
                }}
              >
                {
                  diagnoseFile && (
                    <span style={{marginRight: 5}}>Network Log file:</span>
                  )
                }
                <div style={{flex: 1}}>
                  {
                    diagnoseFile && (
                      <Input
                        value={diagnoseFile}
                        readOnly
                        style={{width: '100%'}}
                      />
                    )
                  }
                  {diagnoseState}
                </div>
              </div>
            )
          }
        </div>
      </Modal>
    );
  }
}

Configuration.propTypes = {
  visible: PropTypes.bool,
  onClose: PropTypes.func,
  fileSystem: PropTypes.object
};

export default Configuration;
