/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.room.rpc;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionWrapper {
  private static final Logger log = LoggerFactory.getLogger(SessionWrapper.class);

  private Session session;
  private ConcurrentMap<Integer, Transaction> transactions = new ConcurrentHashMap<Integer, Transaction>();

  public SessionWrapper(Session session) {
    this.session = session;
  }

  public Session getSession() {
    return session;
  }

  public Transaction getTransaction(Integer requestId) {
    return transactions.get(requestId);
  }

  public void addTransaction(Integer requestId, Transaction t) {
    Transaction oldT = transactions.putIfAbsent(requestId, t);
    if (oldT != null) {
      log.error("Found an existing transaction for the key {}", requestId);
    }
  }

  public void removeTransaction(Integer requestId) {
    transactions.remove(requestId);
  }

  public Collection<Transaction> getTransactions() {
    return transactions.values();
  }
}
