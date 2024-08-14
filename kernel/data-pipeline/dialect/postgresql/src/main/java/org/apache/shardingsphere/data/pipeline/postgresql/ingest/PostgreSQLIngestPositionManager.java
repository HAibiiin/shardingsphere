/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.data.pipeline.postgresql.ingest;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.data.pipeline.core.exception.PipelineInternalException;
import org.apache.shardingsphere.data.pipeline.core.ingest.position.DialectIngestPositionManager;
import org.apache.shardingsphere.data.pipeline.postgresql.ingest.pojo.ReplicationSlotInfo;
import org.apache.shardingsphere.data.pipeline.postgresql.ingest.wal.WALPosition;
import org.apache.shardingsphere.data.pipeline.postgresql.ingest.wal.decode.PostgreSQLLogSequenceNumber;
import org.postgresql.replication.LogSequenceNumber;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Ingest position manager for PostgreSQL.
 */
@Slf4j
public final class PostgreSQLIngestPositionManager implements DialectIngestPositionManager {
    
    private static final String DECODE_PLUGIN = "test_decoding";
    
    private static final String DUPLICATE_OBJECT_ERROR_CODE = "42710";
    
    @Override
    public WALPosition init(final String data) {
        return new WALPosition(new PostgreSQLLogSequenceNumber(LogSequenceNumber.valueOf(data)));
    }
    
    @Override
    public WALPosition init(final DataSource dataSource, final String slotNameSuffix) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            createSlotIfNotExist(connection, PostgreSQLSlotNameGenerator.getUniqueSlotName(connection, slotNameSuffix));
            return getWALPosition(connection);
        }
    }
    
    private void createSlotIfNotExist(final Connection connection, final String slotName) throws SQLException {
        Optional<ReplicationSlotInfo> slotInfo = getSlotInfo(connection, slotName);
        if (!slotInfo.isPresent()) {
            createSlot(connection, slotName);
            return;
        }
        if (null == slotInfo.get().getDatabaseName()) {
            dropSlotIfExist(connection, slotName);
            createSlot(connection, slotName);
        }
    }
    
    private Optional<ReplicationSlotInfo> getSlotInfo(final Connection connection, final String slotName) throws SQLException {
        String sql = "SELECT slot_name, database FROM pg_replication_slots WHERE slot_name=? AND plugin=?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, slotName);
            preparedStatement.setString(2, DECODE_PLUGIN);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() ? Optional.of(new ReplicationSlotInfo(resultSet.getString(1), resultSet.getString(2))) : Optional.empty();
            }
        }
    }
    
    private void createSlot(final Connection connection, final String slotName) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM pg_create_logical_replication_slot(?, ?)")) {
            preparedStatement.setString(1, slotName);
            preparedStatement.setString(2, DECODE_PLUGIN);
            preparedStatement.execute();
        } catch (final SQLException ex) {
            if (!DUPLICATE_OBJECT_ERROR_CODE.equals(ex.getSQLState())) {
                throw ex;
            }
        }
    }
    
    private void dropSlotIfExist(final Connection connection, final String slotName) throws SQLException {
        if (!getSlotInfo(connection, slotName).isPresent()) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT pg_drop_replication_slot(?)")) {
            preparedStatement.setString(1, slotName);
            preparedStatement.execute();
        }
    }
    
    private WALPosition getWALPosition(final Connection connection) throws SQLException {
        try (
                PreparedStatement preparedStatement = connection.prepareStatement(getLogSequenceNumberSQL(connection));
                ResultSet resultSet = preparedStatement.executeQuery()) {
            resultSet.next();
            return new WALPosition(new PostgreSQLLogSequenceNumber(LogSequenceNumber.valueOf(resultSet.getString(1))));
        }
    }
    
    private String getLogSequenceNumberSQL(final Connection connection) throws SQLException {
        if (9 == connection.getMetaData().getDatabaseMajorVersion() && 6 <= connection.getMetaData().getDatabaseMinorVersion()) {
            return "SELECT PG_CURRENT_XLOG_LOCATION()";
        }
        if (10 <= connection.getMetaData().getDatabaseMajorVersion()) {
            return "SELECT PG_CURRENT_WAL_LSN()";
        }
        throw new PipelineInternalException("Unsupported PostgreSQL version: " + connection.getMetaData().getDatabaseProductVersion());
    }
    
    @Override
    public void destroy(final DataSource dataSource, final String slotNameSuffix) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            dropSlotIfExist(connection, PostgreSQLSlotNameGenerator.getUniqueSlotName(connection, slotNameSuffix));
        }
    }
    
    @Override
    public String getDatabaseType() {
        return "PostgreSQL";
    }
}
