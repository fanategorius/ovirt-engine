#
# ovirt-engine-setup -- ovirt engine setup
# Copyright (C) 2013-2015 Red Hat, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


"""Cinderlib Misc plugin."""


import gettext

from otopi import constants as otopicons
from otopi import filetransaction
from otopi import plugin
from otopi import util

from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.cinderlib import constants as oclcons
from ovirt_engine_setup.engine import constants as oenginecons
from ovirt_engine_setup.engine_common import constants as oengcommcons


def _(m):
    return gettext.dgettext(message=m, domain='ovirt-engine-setup')


@util.export
class Plugin(plugin.PluginBase):
    """Cinderlib Misc plugin."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)
        self._enabled = False

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        after=(
            oclcons.Stages.DB_CL_SCHEMA,
        ),
        condition=lambda self: (
            self.environment[oenginecons.CoreEnv.ENABLE]
        ),
    )
    def _misc(self):
        self.environment[otopicons.CoreEnv.MAIN_TRANSACTION].append(
            filetransaction.FileTransaction(
                name=(
                    oclcons.FileLocations.
                    OVIRT_ENGINE_SERVICE_CONFIG_CINDERLIB_MISC
                ),
                mode=0o640,
                owner=self.environment[oengcommcons.SystemEnv.USER_ROOT],
                group=self.environment[osetupcons.SystemEnv.GROUP_ENGINE],
                enforcePermissions=True,
                content=self.get_cinderlib_db_enable_content(),
                modifiedList=self.environment[
                    otopicons.CoreEnv.MODIFIED_FILES
                ],
            )
        )

    def get_cinderlib_db_enable_content(self):
        return (
            '{prefix}_DB_ENABLE="{enable}"\n'.format(
                prefix="CINDERLIB",
                enable=self.environment[oclcons.CoreEnv.ENABLE]
            )
        )
